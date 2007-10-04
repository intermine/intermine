package org.flymine.web.widget;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
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
import org.intermine.objectstore.query.SimpleConstraint;

import org.intermine.bio.web.logic.BioUtil;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.widget.EnrichmentWidgetLdr;

import org.flymine.model.genomic.DataSet;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.IntergenicRegion;
import org.flymine.model.genomic.Motif;
import org.flymine.model.genomic.Organism;
import org.flymine.model.genomic.TFBindingSite;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
/**
 * @author Julie Sullivan
 */
public class TiffinLdr implements EnrichmentWidgetLdr
{
    Query sampleQuery;
    Query populationQuery;
    Collection organisms;
    int total;
    String externalLink;
    String append; 
    
    /**
     * @param request The HTTP request we are processing
     */
     public TiffinLdr(HttpServletRequest request) {

             HttpSession session = request.getSession();
             Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
             ServletContext servletContext = session.getServletContext();
             ObjectStoreInterMineImpl os =
                 (ObjectStoreInterMineImpl) servletContext.getAttribute(Constants.OBJECTSTORE);

             String bagName = request.getParameter("bagName");
             Map<String, InterMineBag> allBags =
                 WebUtil.getAllBags(profile.getSavedBags(), servletContext);
             InterMineBag bag = allBags.get(bagName);
                        
             sampleQuery = getQuery(os, bag, true);
             populationQuery = getQuery(os, bag, false);
     }
     

     /**
      * @return the query representing the sample population (the bag)
      */
     public Query getSample() {
         return sampleQuery;
     }
     
     /**
      * @return the query representing the entire population (all the items in the database)
      */
     public Query getPopulation() {
         return populationQuery;
     }

     /**
      * 
      * @param os
      * @param bag
      * @return description of reference population, ie "Accounting dept"
      */
     public Collection getReferencePopulation() {
         return organisms;
     }
     
     /** 
      * @param os     
      * @return the query representing the sample population (the bag)
      */
     public int getTotal(ObjectStore os) {
         return BioUtil.getGeneTotal(os, organisms);
     }

     private Query getQuery(ObjectStore os, InterMineBag bag, boolean useBag) {
         Query subQ = new Query();
         subQ.setDistinct(true);
         QueryClass qcGene = new QueryClass(Gene.class);
         QueryClass qcIntergenicRegion = new QueryClass(IntergenicRegion.class);
         QueryClass qcTFBindingSite = new QueryClass(TFBindingSite.class);
         QueryClass qcDataSet = new QueryClass(DataSet.class);
         QueryClass qcMotif = new QueryClass(Motif.class);
         QueryClass qcOrganism = new QueryClass(Organism.class);

         QueryField qfGeneId = new QueryField(qcGene, "id");
         QueryField qfOrganismName = new QueryField(qcOrganism, "name");
         QueryField qfId = new QueryField(qcMotif, "identifier");
         QueryField qfDataSet = new QueryField(qcDataSet, "title");

         subQ.addFrom(qcGene);
         subQ.addFrom(qcIntergenicRegion);
         subQ.addFrom(qcTFBindingSite);
         subQ.addFrom(qcDataSet);
         subQ.addFrom(qcMotif);
         subQ.addFrom(qcOrganism);

         subQ.addToSelect(qfId);
         subQ.addToSelect(qfGeneId);

         ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
         if (useBag) {
             // genes must be in bag
             BagConstraint bc1 = new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getOsb());
             cs.addConstraint(bc1);
         }
         // get organisms
         organisms = BioUtil.getOrganisms(os, bag);

         // limit to organisms in the bag
         BagConstraint bc2 = new BagConstraint(qfOrganismName, ConstraintOp.IN, organisms);
         cs.addConstraint(bc2);

         // gene is from organism
         QueryObjectReference qr1 = new QueryObjectReference(qcGene, "organism");
         ContainsConstraint cc1 = 
             new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qcOrganism);
         cs.addConstraint(cc1);

         QueryObjectReference qr2 = 
             new QueryObjectReference(qcGene, "upstreamIntergenicRegion");
         ContainsConstraint cc2 = 
             new ContainsConstraint(qr2, ConstraintOp.CONTAINS, qcIntergenicRegion);
         cs.addConstraint(cc2);

         QueryCollectionReference qr3 = 
             new QueryCollectionReference(qcIntergenicRegion, "overlappingFeatures");
         ContainsConstraint cc3 = 
             new ContainsConstraint(qr3, ConstraintOp.CONTAINS, qcTFBindingSite);
         cs.addConstraint(cc3);

         QueryCollectionReference qr4 = 
             new QueryCollectionReference(qcTFBindingSite, "evidence");
         ContainsConstraint cc4 = new ContainsConstraint(qr4, ConstraintOp.CONTAINS, qcDataSet);
         cs.addConstraint(cc4);

         QueryObjectReference  qr5 = new QueryObjectReference(qcTFBindingSite, "motif");
         ContainsConstraint cc5 = new ContainsConstraint(qr5, ConstraintOp.CONTAINS, qcMotif);
         cs.addConstraint(cc5);

         SimpleConstraint sc = 
             new SimpleConstraint(qfDataSet, ConstraintOp.EQUALS, new QueryValue("Tiffin"));
         cs.addConstraint(sc);

         subQ.setConstraint(cs);
     
         Query q = new Query();
         q.addFrom(subQ);
         
         QueryFunction geneCount = new QueryFunction();
         
         QueryField qfMotif = new QueryField(subQ, qfId);
         
         q.addToSelect(qfMotif);
         q.addToSelect(geneCount);
         if (useBag) { 
             q.addToSelect(qfMotif);
         }
         
         q.addToGroupBy(qfMotif);  
         
         return q;
     }
     
     /**
      * @return if the widget should have an external link, where it should go to
      */
     public String getExternalLink() {
         return externalLink;
     }
     
     /**
      * 
      * @return the string to append to the end of external link
      */
     public String getAppendage() {
         return append;
     }
}



