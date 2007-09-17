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

import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;

import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Organism;
import org.flymine.model.genomic.Publication;
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
 * @author Julie Sullivan
 */
public class PublicationDisplayerController extends TilesAction
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
     public ActionForward execute(@SuppressWarnings("unused") ActionMapping mapping,
                                  @SuppressWarnings("unused") ActionForm form,
                                  HttpServletRequest request,
                                  @SuppressWarnings("unused") HttpServletResponse response)
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
                         
             // put in request for display on the .jsp page
             request.setAttribute("bagName", bagName);
             
             // build query constrained by bag
             Query querySample = new Query();
             querySample.setDistinct(false);
             QueryClass qcGene = new QueryClass(Gene.class);
             QueryClass qcPub = new QueryClass(Publication.class);
             QueryClass qcOrganism = new QueryClass(Organism.class);
          
             QueryField qfGeneId = new QueryField(qcGene, "id");
             QueryField qfOrganismName = new QueryField(qcOrganism, "name");
             QueryField qfId = new QueryField(qcPub, "pubMedId");
             QueryField qfPubTitle = new QueryField(qcPub, "title");
             
             QueryFunction geneCount = new QueryFunction();

             querySample.addFrom(qcGene);
             querySample.addFrom(qcPub);
             querySample.addFrom(qcOrganism);
             
             querySample.addToSelect(qfId);
             querySample.addToSelect(geneCount);
             querySample.addToSelect(qfPubTitle);

             ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);

             if (bag != null) {
                 // genes must be in bag
                 BagConstraint bc1 =
                     new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getOsb());
                 cs1.addConstraint(bc1);
             } else {
                 // always need a bag!
                 throw new Exception("Need a bag to calculate stats!  Bad user!");
             }

             // get organisms
             ArrayList organisms = (ArrayList) FlymineUtil.getOrganisms(os, bag);

             // limit to organisms in the bag
             BagConstraint bc2 = new BagConstraint(qfOrganismName, ConstraintOp.IN, organisms);
             cs1.addConstraint(bc2);

             // gene is from organism
             QueryObjectReference qr1 = new QueryObjectReference(qcGene, "organism");
             ContainsConstraint cc1 
                                 = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qcOrganism);
             cs1.addConstraint(cc1);
             
             // gene.Proteins CONTAINS pub
             QueryCollectionReference qr2 = new QueryCollectionReference(qcGene, "publications");
             ContainsConstraint cc2 =
                 new ContainsConstraint(qr2, ConstraintOp.CONTAINS, qcPub);
             cs1.addConstraint(cc2);
             querySample.setConstraint(cs1);
             
             querySample.addToGroupBy(qfId);             
             querySample.addToGroupBy(qfPubTitle);
             
             // construct population query
             Query queryPopulation = new Query();
             queryPopulation.setDistinct(false);

             queryPopulation.addFrom(qcGene);
             queryPopulation.addFrom(qcPub);
             queryPopulation.addFrom(qcOrganism);
            
             queryPopulation.addToSelect(qfId);
             queryPopulation.addToSelect(geneCount);
              
             ConstraintSet cs2 = new ConstraintSet(ConstraintOp.AND);
             cs2.addConstraint(cc1);
             cs2.addConstraint(cc2);      
             cs2.addConstraint(bc2);
             queryPopulation.setConstraint(cs2);
             
             queryPopulation.addToGroupBy(qfId);
                          
             // run both queries and compare the results 
             ArrayList results = FlymineUtil.statsCalc(os, queryPopulation, querySample, bag, 
                                       organisms, maxValue);
             if (results.isEmpty()) {
                 return null;
             }
             request.setAttribute("pvalues", results.get(0));
             request.setAttribute("totals", results.get(1));
             request.setAttribute("names", results.get(2));
             request.setAttribute("organisms", "All genes from:  " + organisms.toString());
             return null;
         } catch (Exception e) {
             request.setAttribute("organisms", "UNKNOWN (" + e + ")");
             throw new Exception(e);
         }
     }
/*     SELECT a2_.pubMedId AS a4_, COUNT(*) AS a5_ FROM org.flymine.model.genomic.Gene AS a1_, 
     org.flymine.model.genomic.Publication AS a2_, org.flymine.model.genomic.Organism AS a3_ 
     WHERE (a1_.organism CONTAINS a3_ 
     AND a1_.publications CONTAINS a2_
      AND a3_.name IN ?) 1: [Drosophila melanogaster]

     SELECT a2_.pubMedId AS a4_, COUNT(*) AS a5_ 
     FROM org.flymine.model.genomic.Gene AS a1_, 
     org.flymine.model.genomic.Publication AS a2_,
     org.flymine.model.genomic.Organism AS a3_ 
     WHERE (a1_.id IN BAG(969032704) 
     AND a3_.name IN ? 
     AND a1_.organism CONTAINS a3_ 
     AND a1_.publications CONTAINS a2_) 
     GROUP BY a2_.pubMedId 1: [Drosophila melanogaster]
     
     SELECT DISTINCT a3_, a1_ FROM org.flymine.model.genomic.Gene AS a1_, 
     org.flymine.model.genomic.Organism AS a2_, 
     org.flymine.model.genomic.Publication AS a3_ 
     WHERE (
     a1_.id IN BAG(783032706) 
     AND a1_.organism CONTAINS a2_ 
     AND LOWER(a2_.shortName) = 'd. melanogaster' 
     AND a1_.publications CONTAINS a3_
     ) 
     ORDER BY a3_.pubMedId, a1_.identifier

     
*/
}



