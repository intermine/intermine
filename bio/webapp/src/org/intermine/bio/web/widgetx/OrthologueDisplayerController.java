package org.intermine.bio.web.widget;

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
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.BagElement;

import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Organism;
import org.flymine.model.genomic.Orthologue;

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
 * Controller to get all of the orthologues for all genes in a bag.
 * 
 * @author Julie Sullivan
 */
public class OrthologueDisplayerController extends TilesAction
{
    
    /**
     * @see TilesAction#execute
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
                
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);

/*
 * SELECT DISTINCT a1_, a3_ FROM org.flymine.model.genomic.Gene AS a1_,
 * org.flymine.model.genomic.Orthologue AS a2_, org.flymine.model.genomic.Gene AS a3_ WHERE
 * (a1_.orthologues CONTAINS a2_ AND a2_.subject CONTAINS a3_)
 */

        Query q = new Query();
        
        QueryClass subjectGeneQC = new QueryClass(Gene.class);
        QueryClass objectGeneQC = new QueryClass(Gene.class);
        QueryClass orthologueQC = new QueryClass(Orthologue.class);
        QueryClass organismQC = new QueryClass(Organism.class);
            
        QueryField qfTaxonId = new QueryField(organismQC, "taxonId"); 
        // with no args, automatically is COUNT
        QueryFunction geneCountQF = new QueryFunction();
        
        // FROM statement        
        q.addFrom(subjectGeneQC);
        q.addFrom(objectGeneQC);
        q.addFrom(orthologueQC);
        q.addFrom(organismQC);
                    
        // SELECT statement
        q.addToSelect(qfTaxonId);
        q.addToSelect(geneCountQF);
        
        // set of constraints
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        
        // orthologues
        QueryCollectionReference qr1 = new QueryCollectionReference(objectGeneQC, "orthologues");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, orthologueQC);
        cs.addConstraint(cc1);
      
        // orthologue record has to contain subject gene
        QueryObjectReference qr2 = new QueryObjectReference(orthologueQC, "subject");
        ContainsConstraint cc2 = new ContainsConstraint(qr2, ConstraintOp.CONTAINS, subjectGeneQC);
        cs.addConstraint(cc2);
       
        // object gene must be in bag
        QueryField qf1 = new QueryField(objectGeneQC, "id");
        Collection bag = (Collection) request.getAttribute("bag");
        QueryObjectReference qr3 = new QueryObjectReference(subjectGeneQC, "organism");
        ContainsConstraint cc3 = new ContainsConstraint(qr3, ConstraintOp.CONTAINS, organismQC);
        cs.addConstraint(cc3);
        

        
        if (bag != null && !bag.isEmpty()) {
            // get Ids of genes in the bag
            Collection geneIds = new ArrayList();
            Iterator it = bag.iterator();
            while (it.hasNext()) {
                BagElement bagElement = (BagElement) it.next();
                geneIds.add(bagElement.getId());
            }
            BagConstraint bc = new BagConstraint(qf1, ConstraintOp.IN, geneIds);
            cs.addConstraint(bc); 

        // or if we're on the object details page, constrain to be that gene only
        } else {
            String s = request.getParameter("id");
            int n = Integer.parseInt(s);
            Integer nn = new Integer(n);
            SimpleConstraint bc = new SimpleConstraint(new QueryField(objectGeneQC, "id"), 
                                  ConstraintOp.EQUALS, new QueryValue(nn));    
            cs.addConstraint(bc);
           
        }
      
        q.setConstraint(cs);
        
        // group by organism
        q.addToGroupBy(qfTaxonId);
                
        Results r1 = os.execute(q);
   
        // get organisms
        Query qOrganisms = new Query();
        QueryClass qcOrganism = new QueryClass(Organism.class);
        qfTaxonId = new QueryField(qcOrganism, "taxonId");
        qOrganisms.addFrom(qcOrganism);
        qOrganisms.addToSelect(qfTaxonId);
        
        /*          
         * TODO get organisms from oss instead of querying everytime.
        PathNode pathNode = new PathNode("org.flymine.model.genomic.Organism");
        ObjectStoreSummary oss = (ObjectStoreSummary) servletContext.
            getAttribute(Constants.OBJECT_STORE_SUMMARY);
        DisplayConstraint dispConst =  new DisplayConstraint(pathNode, os.getModel(), oss, null);
        */
        
        Results r2 = os.execute(qOrganisms);
        
         /*  Map to hold results in 
          taxonId = key
          array = value
          [orthologue count][query for results page][id]              
          */
        Map resultsMap = new HashMap();
        
        Iterator it1 = r1.iterator();   // orthologues
        Iterator it2 = r2.iterator();   // organisms
        
        ResultsRow rr1 = null;        
        if (it1.hasNext()) {
            rr1 = (ResultsRow) it1.next();
        }     
   
        // loop through organisms.  add orthologues for organism, if present
        while (it2.hasNext()) {
            
            // orthologue count for this organism
            ResultsRow rr2 =  (ResultsRow) it2.next();
            // array to hold results
            String[] a = new String[3];
            // id of this organism
            Integer taxonId = (java.lang.Integer) rr2.get(0);
                 
            // match - this organism has orthologues
            if (rr1 != null && rr2.get(0).equals(rr1.get(0))) {
                                      
                // count of orthologues for this organism
                Long orthoCount = (java.lang.Long) rr1.get(1);                
                // add to our results array
                a[0] = orthoCount.toString();  
                               
                // build organism-specific query to be used on results page
                Query qs = new Query();
                
                // FROM statement        
                qs.addFrom(subjectGeneQC);
                qs.addFrom(objectGeneQC);
                qs.addFrom(orthologueQC);
                qs.addFrom(organismQC);

                // SELECT statement
                qs.addToSelect(subjectGeneQC);

                // set of constraints
                cs = new ConstraintSet(ConstraintOp.AND);

                // WHERE statement
                cs.addConstraint(cc1);
                cs.addConstraint(cc2);
                cs.addConstraint(cc3);

                if (bag == null || bag.isEmpty()) {
                    String s = request.getParameter("id");
                    int n = Integer.parseInt(s);
                    Integer nn = new Integer(n);
                    SimpleConstraint bc = new SimpleConstraint(new QueryField(objectGeneQC, "id"), 
                                          ConstraintOp.EQUALS, new QueryValue(nn));    
                    cs.addConstraint(bc);
                   
                }
             
                // make sure organism matches this one in this loop
                SimpleConstraint cc4 = new 
                SimpleConstraint(new QueryField(organismQC, "taxonId"), 
                                 ConstraintOp.EQUALS, new QueryValue(taxonId));
                cs.addConstraint(cc4);

                // add constraints
                qs.setConstraint(cs);

                // add new query to results
                a[1] = qs.toString();
                
                // if there is only one results, get the ID of that object
                if (a[0].equals("1")) {

                    // need the id of the object    
                    a[2] = getId(os, qs, new QueryField(subjectGeneQC, "id"));
                    
                }
                                
                // move to next organism in results
                if (it1.hasNext()) {
                    rr1 =  (ResultsRow) it1.next();
                }    
                
            // no match 
            } else {              
                a[0] = "0";              
            }
            // add results to map
            resultsMap.put(taxonId, a);            
        }            
        request.setAttribute("orthos", resultsMap);     
        return null;
    }
    
    private String getId(ObjectStore os, Query q, QueryField geneId) {
        
        String idString = null;
        // select the id of the gene
        q.addToSelect(geneId);
        q.addToGroupBy(geneId);
        Results r = os.execute(q);
        
        Iterator it = r.iterator();   
    
        if (it.hasNext()) {
            ResultsRow rr =  (ResultsRow) it.next();
            Integer n = (Integer) rr.get(1);
            idString = n.toString();
        }
        return idString;
        
    }
    
}
