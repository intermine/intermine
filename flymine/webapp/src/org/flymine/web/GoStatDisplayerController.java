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
import java.util.Map;

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
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;

import org.flymine.model.genomic.GOAnnotation;
import org.flymine.model.genomic.GOTerm;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Organism;
import org.flymine.web.logic.FlymineUtil;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * calculates p-values of goterms
 * @author Julie Sullivan
 */
public class GoStatDisplayerController extends TilesAction
{

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

         try {
             HttpSession session = request.getSession();
             Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
             ServletContext servletContext = session.getServletContext();
             ObjectStoreInterMineImpl os =
                 (ObjectStoreInterMineImpl) servletContext.getAttribute(Constants.OBJECTSTORE);

             String bagName = request.getParameter("bagName");
             Map<String, InterMineBag> allBags =
                 WebUtil.getAllBags(profile.getSavedBags(), servletContext);
             InterMineBag bag = allBags.get(bagName);
             
             // TODO get these from request form
             Double maxValue = new Double("0.10");
             String significanceValue = (request.getParameter("significanceValue") != null 
                                       ? request.getParameter("significanceValue") : "0.05");
             
             // TODO get these from properties files
             String namespace = (request.getParameter("ontology") != null
                               ? request.getParameter("ontology") : "biological_process");
             
             // put in request for display on the .jsp page
             request.setAttribute("bagName", bagName);
             request.setAttribute("ontology", namespace);
             
             // list of ontologies to ignore
             Collection badOntologies = getOntologies(); 

             // build query constrained by bag
             Query querySample = new Query();
             querySample.setDistinct(false);
             QueryClass qcGene = new QueryClass(Gene.class);
             QueryClass qcGoAnnotation = new QueryClass(GOAnnotation.class);
             QueryClass qcOrganism = new QueryClass(Organism.class);
             QueryClass qcGo = new QueryClass(GOTerm.class);

             QueryField qfQualifier = new QueryField(qcGoAnnotation, "qualifier");
             QueryField qfGoTerm = new QueryField(qcGoAnnotation, "name");
             QueryField qfGeneId = new QueryField(qcGene, "id");
             QueryField qfNamespace = new QueryField(qcGo, "namespace");
             QueryField qfGoTermId = new QueryField(qcGo, "identifier");
             QueryField qfOrganismName = new QueryField(qcOrganism, "name");

             QueryFunction geneCount = new QueryFunction();

             querySample.addFrom(qcGene);
             querySample.addFrom(qcGoAnnotation);
             querySample.addFrom(qcOrganism);
             querySample.addFrom(qcGo);

             querySample.addToSelect(qfGoTermId);
             querySample.addToSelect(geneCount);
             querySample.addToSelect(qfGoTerm);

             ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

             if (bag != null) {
                 // genes must be in bag
                 BagConstraint bc1 =
                     new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getOsb());
                 cs.addConstraint(bc1);
             } else {
                 // always need a bag!
                 throw new Exception("Need a bag to calculate gostats!  Bad user!");
             }

             // get organisms
             ArrayList organisms = (ArrayList) FlymineUtil.getOrganisms(os, bag);

             // limit to organisms in the bag
             BagConstraint bc2 = new BagConstraint(qfOrganismName, ConstraintOp.IN, organisms);
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
             ContainsConstraint cc2 
                                 = new ContainsConstraint(qr2, ConstraintOp.CONTAINS, qcOrganism);
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
             querySample.setConstraint(cs);
             querySample.addToGroupBy(qfGoTerm);
             querySample.addToGroupBy(qfGoTermId);


             // construct population query
             Query queryPopulation = new Query();
             queryPopulation.setDistinct(false);

             queryPopulation.addFrom(qcGene);
             queryPopulation.addFrom(qcGoAnnotation);
             queryPopulation.addFrom(qcOrganism);
             queryPopulation.addFrom(qcGo);

             queryPopulation.addToSelect(qfGoTermId);
             queryPopulation.addToSelect(geneCount);

             cs = new ConstraintSet(ConstraintOp.AND);
             cs.addConstraint(cc1);
             cs.addConstraint(cc2);
             cs.addConstraint(cc3);
             cs.addConstraint(sc1);
             cs.addConstraint(sc2);
             cs.addConstraint(bc2);
             cs.addConstraint(bc3);
             queryPopulation.setConstraint(cs);

             queryPopulation.addToGroupBy(qfGoTermId);

             // total number of genes for all organisms of interest 
             int geneCountAll = getGeneTotal(os, organisms);
             
             // run both queries and compare the results 
             ArrayList results = FlymineUtil.statsCalc(os, queryPopulation, querySample, bag, 
                                       geneCountAll, maxValue, significanceValue);
             if (results.isEmpty()) {
                 return null;
             }
             request.setAttribute("goStatPvalues", results.get(0));
             request.setAttribute("goStatGeneTotals", results.get(1));
             request.setAttribute("goStatGoTermToId", results.get(2));
             request.setAttribute("goStatOrganisms", "All genes from:  " + organisms.toString());
             return null;
         } catch (Exception e) {
             request.setAttribute("goStatOrganisms", "UNKNOWN");
             return null;
         }
     }

     /* gets total number of genes */
     private int getGeneTotal(ObjectStore os, Collection organisms) {

            Query q = new Query();
            q.setDistinct(false);
            QueryClass qcGene = new QueryClass(Gene.class);
            QueryClass qcOrganism = new QueryClass(Organism.class);

            QueryField qfOrganism = new QueryField(qcOrganism, "name");
            QueryFunction geneCount = new QueryFunction();

            q.addFrom(qcGene);

            q.addFrom(qcOrganism);

            q.addToSelect(geneCount);

            ConstraintSet cs;
            cs = new ConstraintSet(ConstraintOp.AND);

            /* organism is in bag */
            BagConstraint bc2 = new BagConstraint(qfOrganism, ConstraintOp.IN, organisms);
            cs.addConstraint(bc2);

            /* gene is from organism */
            QueryObjectReference qr2 = new QueryObjectReference(qcGene, "organism");
            ContainsConstraint cc2 = new ContainsConstraint(qr2, ConstraintOp.CONTAINS, qcOrganism);
            cs.addConstraint(cc2);

            q.setConstraint(cs);

            Results r = os.execute(q);
            Iterator it = r.iterator();
            ResultsRow rr =  (ResultsRow) it.next();
            Long l = (java.lang.Long) rr.get(0);
            int n = l.intValue();
            return n;
        }



        // adds 3 main ontologies to array.  these 3 will be excluded from the query
        private Collection getOntologies() {

            Collection<String> ids = new ArrayList<String>();

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
