package org.flymine.web;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;
import org.flymine.model.genomic.GOAnnotation;
import org.flymine.model.genomic.GOTerm;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Organism;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.Profile;
import org.intermine.web.logic.SortableMap;
import org.intermine.web.logic.bag.InterMineBag;

/**
 * calculates p-values of goterms
 * @author Julie Sullivan
 */
public class GoStatDisplayerController extends TilesAction
{

    String organismNames;

    /**
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
     public ActionForward execute(ActionMapping mapping,
                                  ActionForm form,
                                  HttpServletRequest request,
                                  HttpServletResponse response)
                    throws Exception {


            HttpSession session = request.getSession();
            Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
            ServletContext servletContext = session.getServletContext();
            ObjectStoreInterMineImpl os =
                (ObjectStoreInterMineImpl) servletContext.getAttribute(Constants.OBJECTSTORE);
            ObjectStoreSummary oss =
                (ObjectStoreSummary) servletContext.getAttribute(Constants.OBJECT_STORE_SUMMARY);

            session.removeAttribute("goStatPvalues");
            session.removeAttribute("goStatGeneTotals");
            session.removeAttribute("goStatGoTermToId");
            session.removeAttribute("goStatOrganisms");

            String significanceValue = null;
            significanceValue = request.getParameter("significanceValue");
            if (significanceValue == null)  {
                significanceValue = new String("0.05");
            }
            String namespace = null;
            namespace = request.getParameter("ontology");
            if (namespace == null)  {
                 namespace = new String("biological_process");
            }
            Double maxValue = new Double("0.10");
            String bagName = request.getParameter("bagName");
            InterMineBag bag = (InterMineBag) profile.getSavedBags().get(bagName);

            // put all vars in request for getting on the .jsp page
            request.setAttribute("bagName", bagName);
            request.setAttribute("ontology", namespace);

            Collection organisms = getOrganisms(os, bag);
            Collection badOntologies = getOntologies(); // list of ontologies to ignore

            // ~~~~~~~~~~~~~~~~~~ BAG QUERY ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            Query q = new Query();
            q.setDistinct(false);
            QueryClass qcGene = new QueryClass(Gene.class);
            QueryClass qcGoAnnotation = new QueryClass(GOAnnotation.class);
            QueryClass qcOrganism = new QueryClass(Organism.class);
            QueryClass qcGo = new QueryClass(GOTerm.class);

            QueryField qfQualifier = new QueryField(qcGoAnnotation, "qualifier");
            QueryField qfGoTerm = new QueryField(qcGoAnnotation, "name");
            QueryField qfGeneId = new QueryField(qcGene, "id");
            QueryField qfOrganism = new QueryField(qcOrganism, "id");
            QueryField qfNamespace = new QueryField(qcGo, "namespace");
            QueryField qfGoTermId = new QueryField(qcGo, "identifier");

            QueryFunction geneCount = new QueryFunction();

            q.addFrom(qcGene);
            q.addFrom(qcGoAnnotation);
            q.addFrom(qcOrganism);
            q.addFrom(qcGo);

            q.addToSelect(qfGoTermId);
            q.addToSelect(geneCount);
            q.addToSelect(qfGoTerm);

            ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

            if (bag != null) {
                // genes must be in bag
                BagConstraint bc1 =
                    new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getListOfIds());
                cs.addConstraint(bc1);
            }

            // limit to organisms in the bag
            BagConstraint bc2 = new BagConstraint(qfOrganism, ConstraintOp.IN, organisms);
            cs.addConstraint(bc2);

            // ignore main 3 ontologies
            BagConstraint bc3 = new BagConstraint(qfGoTermId, ConstraintOp.NOT_IN, badOntologies);
            cs.addConstraint(bc3);


            // gene.goAnnotation CONTAINS GOAnnotation
            QueryCollectionReference qr1 = new QueryCollectionReference(qcGene, "allGoAnnotation");
            ContainsConstraint cc1 =
                new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qcGoAnnotation);
            cs.addConstraint(cc1);

            // gene is from organism
            QueryObjectReference qr2 = new QueryObjectReference(qcGene, "organism");
            ContainsConstraint cc2 = new ContainsConstraint(qr2, ConstraintOp.CONTAINS, qcOrganism);
            cs.addConstraint(cc2);

            // goannotation contains go term
            QueryObjectReference qr3 = new QueryObjectReference(qcGoAnnotation, "property");
            ContainsConstraint cc3 = new ContainsConstraint(qr3, ConstraintOp.CONTAINS, qcGo);
            cs.addConstraint(cc3);

            // can't be a NOT relationship!
            SimpleConstraint sc1 = new SimpleConstraint(qfQualifier,
                                                         ConstraintOp.IS_NULL);
            cs.addConstraint(sc1);

            // go term is of the specified namespace
            SimpleConstraint sc2 = new SimpleConstraint(qfNamespace,
                                                       ConstraintOp.EQUALS,
                                                       new QueryValue(namespace));
            cs.addConstraint(sc2);

            q.setConstraint(cs);

            q.addToGroupBy(qfGoTerm);
            q.addToGroupBy(qfGoTermId);

            Results rBag = new Results(q, os, os.getSequence());

            Iterator itBag = rBag.iterator();
            HashMap geneCountMap = new HashMap();
            HashMap goTermToIdMap = new HashMap();

            // rattle through go terms for genes in bag
            while (itBag.hasNext()) {

                // extract results
                ResultsRow rrBag =  (ResultsRow) itBag.next();

                String goTermIdBag = (String) rrBag.get(0);
                Long countBag = (java.lang.Long) rrBag.get(1);  // put all vars in request for getting on the .jsp page

                // put in our map
                geneCountMap.put(goTermIdBag, countBag);

                // go term id & term, used to display on results page
                goTermToIdMap.put(goTermIdBag, rrBag.get(2));

            }

            // ~~~~~~~~~~~~~~~~~~~~~~~ ALL QUERY ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            q = new Query();
            q.setDistinct(false);

            q.addFrom(qcGene);
            q.addFrom(qcGoAnnotation);
            q.addFrom(qcOrganism);
            q.addFrom(qcGo);

            q.addToSelect(qfGoTermId);
            q.addToSelect(geneCount);

            cs = new ConstraintSet(ConstraintOp.AND);
            cs.addConstraint(cc1);
            cs.addConstraint(cc2);
            cs.addConstraint(cc3);
            cs.addConstraint(sc1);
            cs.addConstraint(sc2);
            cs.addConstraint(bc2);
            cs.addConstraint(bc3);
            q.setConstraint(cs);

            q.addToGroupBy(qfGoTermId);

            int geneCountAll = getGeneTotal(os, organisms);
            int geneCountBag = bag.size();

            Results rAll = new Results(q, os, os.getSequence());
            rAll.setBatchSize(5000);

            Iterator itAll = rAll.iterator();

            Hypergeometric h = new Hypergeometric(geneCountAll);

            HashMap goGeneCountBagMap = new HashMap();
            HashMap resultsMap = new HashMap();

            while (itAll.hasNext()) {

                ResultsRow rrAll =  (ResultsRow) itAll.next();
                // goterm identifier (ie GO:0000001, etc)
                String goTerm = (String) rrAll.get(0);

                if (geneCountMap.containsKey(goTerm)) {

                    // get counts
                    Long countBag = (Long) geneCountMap.get(goTerm);
                    int goGeneCountBag = countBag.intValue();
                    Long countAll = (java.lang.Long) rrAll.get(1);
                    int goGeneCount = countAll.intValue();
                    double p =
                        h.calculateP(geneCountBag, goGeneCountBag, goGeneCount, geneCountAll);

                    // maps can't handle doubles
                    resultsMap.put(goTerm, new Double(p));
                    goGeneCountBagMap.put(goTerm, String.valueOf(goGeneCountBag));
                }
            }

            Bonferroni b = new Bonferroni(resultsMap, significanceValue);
            b.calculate(maxValue);
            HashMap adjustedResultsMap = b.getAdjustedMap();

            SortableMap sortedMap = new SortableMap(adjustedResultsMap);
            sortedMap.sortValues();

            request.setAttribute("goStatPvalues", sortedMap);
            request.setAttribute("goStatGeneTotals", goGeneCountBagMap);
            request.setAttribute("goStatGoTermToId", goTermToIdMap);
            request.setAttribute("goStatOrganisms", organismNames);
            return null;
        }

        // gets total number of genes.
        private int getGeneTotal(ObjectStore os, Collection organisms) {

            Query q = new Query();
            q.setDistinct(false);
            QueryClass qcGene = new QueryClass(Gene.class);
            QueryClass qcOrganism = new QueryClass(Organism.class);

            QueryField qfOrganism = new QueryField(qcOrganism, "id");
            QueryFunction geneCount = new QueryFunction();

            q.addFrom(qcGene);

            q.addFrom(qcOrganism);

            q.addToSelect(geneCount);

            ConstraintSet cs;
            cs = new ConstraintSet(ConstraintOp.AND);


            BagConstraint bc2 = new BagConstraint(qfOrganism, ConstraintOp.IN, organisms);
            cs.addConstraint(bc2);

            // gene is from organism
            QueryObjectReference qr2 = new QueryObjectReference(qcGene, "organism");
            ContainsConstraint cc2 = new ContainsConstraint(qr2, ConstraintOp.CONTAINS, qcOrganism);
            cs.addConstraint(cc2);

            q.setConstraint(cs);

            Results r = new Results(q, os, os.getSequence());
            Iterator it = r.iterator();
            ResultsRow rr =  (ResultsRow) it.next();
            Long l = (java.lang.Long) rr.get(0);
            int n = l.intValue();
            return n;
        }

        // gets organisms in bag
        private Collection getOrganisms(ObjectStoreInterMineImpl os, InterMineBag bag) {

            Query q = new Query();

            QueryClass qcGene = new QueryClass(Gene.class);
            QueryClass qcOrganism = new QueryClass(Organism.class);

            QueryField qfOrganismName = new QueryField(qcOrganism, "name");
            QueryField qfOrganism = new QueryField(qcOrganism, "id");
            QueryField qfGeneId = new QueryField(qcGene, "id");

            q.addFrom(qcGene);
            q.addFrom(qcOrganism);

            q.addToSelect(qfOrganism);
            q.addToSelect(qfOrganismName);

            ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
            if (bag != null) {
                BagConstraint bc = new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getListOfIds());
                cs.addConstraint(bc);
            }

            QueryObjectReference qr = new QueryObjectReference(qcGene, "organism");
            ContainsConstraint cc = new ContainsConstraint(qr, ConstraintOp.CONTAINS, qcOrganism);
            cs.addConstraint(cc);

            q.setConstraint(cs);

            Results r = new Results(q, os, os.getSequence());
            Iterator it = r.iterator();
            Collection ids = new ArrayList();
            organismNames = null;

            while (it.hasNext()) {

                ResultsRow rr =  (ResultsRow) it.next();
                ids.add(rr.get(0));

                if (organismNames == null) {
                    organismNames = (String) rr.get(1);
                } else {
                    organismNames += ", " + rr.get(1);
                }

            }

            return ids;

        }

        // adds 3 main ontologies to array.  these 3 will be excluded from the query
        private Collection getOntologies() {

            Collection ids = new ArrayList();

            ids.add("GO:0008150");  // biological_process
            ids.add("GO:0003674");  // molecular_function
            ids.add("GO:0005575");  // cellular_component

            return ids;

        }

}



/**
 *
 * 1. query to get a list of go terms in the bag
 *  - total number of genes = N
 *  - total number of genes in bag = K
 * 2. loop through the terms and calc p value
 *  - number of genes with this go term = M
 *  - number of genes with this go term in bag = x
 * 3. calc the probability of this happening
 *
*/
