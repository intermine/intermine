package org.flymine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
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
import java.util.LinkedHashMap;

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

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.web.Constants;
import org.intermine.web.Profile;
import org.intermine.web.SortableMap;
import org.intermine.web.bag.InterMineBag;

import org.flymine.model.genomic.GOAnnotation;
import org.flymine.model.genomic.GOTerm;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Organism;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * calculates p-values of goterms
 * @author Julie Sullivan
 */
public class GoStatDisplayerController extends TilesAction
{

        /**
         * @see TilesAction#execute(ComponentContext, ActionMapping, ActionForm, HttpServletRequest,
         *      HttpServletResponse)
         */
        public ActionForward execute(ComponentContext context, ActionMapping mapping,
                                     ActionForm form,
                                     HttpServletRequest request, HttpServletResponse response)
        throws Exception {

            HttpSession session = request.getSession();
            Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
            ServletContext servletContext = session.getServletContext();
            ObjectStoreInterMineImpl os =
                (ObjectStoreInterMineImpl) servletContext.getAttribute(Constants.OBJECTSTORE);
            ObjectStoreSummary oss =
                (ObjectStoreSummary) servletContext.getAttribute(Constants.OBJECT_STORE_SUMMARY);

            //String significanceValue = new String ("0.05");
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

            LinkedHashMap resultsMap = new LinkedHashMap();
            String bagName = request.getParameter("bagName");
            InterMineBag bag = (InterMineBag) profile.getSavedBags().get(bagName);

            // TODO put this back in if not using rubbish database
            //int geneCountAll = oss.getClassCount("org.flymine.model.genomic.Gene");
            int geneCountAll = getGeneTotal(os);
            int geneCountBag = bag.size();

            Collection organisms = getOrganisms(os, bag);

            // ~~~~~~~~~~~~~~~~~~ BAG QUERY ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            Query q = new Query();
            q.setDistinct(true);
            QueryClass qcGene = new QueryClass(Gene.class);
            QueryClass qcGoAnnotation = new QueryClass(GOAnnotation.class);
            QueryClass qcOrganism = new QueryClass(Organism.class);
            QueryClass qcGo = new QueryClass(GOTerm.class);

            QueryField qfQualifier = new QueryField(qcGoAnnotation, "qualifier");
            QueryField qfGoTerm = new QueryField(qcGoAnnotation, "name");
            QueryField qfGeneId = new QueryField(qcGene, "id");
            QueryField qfOrganism = new QueryField(qcOrganism, "id");
            QueryField qfNamespace = new QueryField(qcGo, "namespace");
            //QueryField qfGoTermId = new QueryField(qcGo, "identifier");

            QueryFunction geneCount = new QueryFunction();

            q.addFrom(qcGene);
            q.addFrom(qcGoAnnotation);
            q.addFrom(qcOrganism);
            q.addFrom(qcGo);

            q.addToSelect(qfGoTerm);
            q.addToSelect(geneCount);

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

            // gene.allGoAnnotation CONTAINS GOAnnotation
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

            Results rBag = new Results(q, os, os.getSequence());

            Iterator itBag = rBag.iterator();
            HashMap geneCountMap = new HashMap();

            // rattle through go terms for genes in bag
            while (itBag.hasNext()) {

                // extract results
                ResultsRow rrBag =  (ResultsRow) itBag.next();

                String goTermIdBag = (String) rrBag.get(0);
                Long countBag = (java.lang.Long) rrBag.get(1);

                // put in our map
                geneCountMap.put(goTermIdBag, countBag);

            }
            // we're done with results from first query
            //os.releaseGoFaster(q);

            // ~~~~~~~~~~~~~~~~~~~~~~~ ALL QUERY ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            q = new Query();
            q.setDistinct(true);

            q.addFrom(qcGene);
            q.addFrom(qcGoAnnotation);
            q.addFrom(qcOrganism);
            q.addFrom(qcGo);

            //.addToSelect(qfGoTermId);
            q.addToSelect(qfGoTerm);
            q.addToSelect(geneCount);

            cs = new ConstraintSet(ConstraintOp.AND);
            cs.addConstraint(cc1);
            cs.addConstraint(cc2);
            cs.addConstraint(cc3);
            cs.addConstraint(sc1);
            cs.addConstraint(sc2);
            cs.addConstraint(bc2);
            q.setConstraint(cs);

            //q.addToGroupBy(qfGoTermId);
            q.addToGroupBy(qfGoTerm);
            //q.addToOrderBy(qfGoTermId);

            try {
                os.goFaster(q);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }

            Results rAll = new Results(q, os, os.getSequence());
            rAll.setBatchSize(5000);

            Iterator itAll = rAll.iterator();

            Hypergeometric h = new Hypergeometric(geneCountAll);

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

                   // get p value
                   // System.out.print("[gene-" + goTermAll + "]");
                   // System.out.print("[geneCountBag-" + geneCountBag + "]");
                   // System.out.print("[goGeneCountBag-" + goGeneCountBag + "]");
                   // System.out.print("[goGeneCount-" + goGeneCount + "]");
                   // System.out.print("[geneCountAll-" + geneCountAll + "]");
                   // System.out.println();
                    double p =
                        h.calculateP(geneCountBag, goGeneCountBag, goGeneCount, geneCountAll);

                    // maps can't handle doubles
                    Double P = new Double(p);

                    resultsMap.put(goTerm, P);

                }
            }
            os.releaseGoFaster(q);

            //Bonferroni b = new Bonferroni(resultsMap, significanceValue);
            //HashMap adjustedResultsMap = b.getAdjustedMap();

            SortableMap sortedMap = new SortableMap(resultsMap);
            sortedMap.sortValues();


            request.setAttribute("pvalues", sortedMap);
            return null;
        }

        // gets total number of genes.
        // TODO should be using object store summary instead!
        private int getGeneTotal(ObjectStore os) {

            Query q = new Query();
            QueryClass qcGene = new QueryClass(Gene.class);
            QueryFunction geneCount = new QueryFunction();
            q.addFrom(qcGene);
            q.addToSelect(geneCount);
            Results r = new Results(q, os, os.getSequence());
            Iterator it = r.iterator();
            ResultsRow rr =  (ResultsRow) it.next();
            Long l = (java.lang.Long) rr.get(0);
            int n = l.intValue();
            return n;
        }

        private Collection getOrganisms(ObjectStoreInterMineImpl os, InterMineBag bag) {

            Query q = new Query();

            QueryClass qcGene = new QueryClass(Gene.class);
            QueryClass qcOrganism = new QueryClass(Organism.class);

            QueryField qfOrganism = new QueryField(qcOrganism, "id");
            QueryField qfGeneId = new QueryField(qcGene, "id");

            q.addFrom(qcGene);
            q.addFrom(qcOrganism);

            q.addToSelect(qfOrganism);

            ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

            BagConstraint bc = new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getListOfIds());
            cs.addConstraint(bc);

            QueryObjectReference qr = new QueryObjectReference(qcGene, "organism");
            ContainsConstraint cc = new ContainsConstraint(qr, ConstraintOp.CONTAINS, qcOrganism);
            cs.addConstraint(cc);

            q.setConstraint(cs);

            Results r = new Results(q, os, os.getSequence());
            Iterator it = r.iterator();
            Collection ids = new ArrayList();

            while (it.hasNext()) {

                ResultsRow rr =  (ResultsRow) it.next();
                ids.add(rr.get(0));
            }

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
