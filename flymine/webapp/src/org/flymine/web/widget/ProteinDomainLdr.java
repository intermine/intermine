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

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.widget.EnrichmentWidgetLdr;

import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Organism;
import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.ProteinFeature;
import org.flymine.web.logic.BioUtil;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @author Julie Sullivan
 */
public class ProteinDomainLdr implements EnrichmentWidgetLdr
{

    Query sampleQuery;
    Query populationQuery;
    Collection organisms;
    int total;
    
    /**
     * @param request The HTTP request we are processing
     */
     public ProteinDomainLdr(HttpServletRequest request) {

        
             HttpSession session = request.getSession();
             Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
             ServletContext servletContext = session.getServletContext();
             ObjectStoreInterMineImpl os =
                 (ObjectStoreInterMineImpl) servletContext.getAttribute(Constants.OBJECTSTORE);

             String bagName = request.getParameter("bagName");
             Map<String, InterMineBag> allBags =
                 WebUtil.getAllBags(profile.getSavedBags(), servletContext);
             InterMineBag bag = allBags.get(bagName);
                          
             // build query constrained by bag
             Query q = new Query();
             q.setDistinct(false);
             QueryClass qcGene = new QueryClass(Gene.class);
             QueryClass qcProtein = new QueryClass(Protein.class);
             QueryClass qcOrganism = new QueryClass(Organism.class);
             QueryClass qcProteinFeature = new QueryClass(ProteinFeature.class);


             QueryField qfGeneId = new QueryField(qcGene, "id");
             QueryField qfName = new QueryField(qcProteinFeature, "name");
             QueryField qfId = new QueryField(qcProteinFeature, "interproId");
             QueryField qfOrganismName = new QueryField(qcOrganism, "name");
             QueryField qfInterpro = new QueryField(qcProteinFeature, "identifier");
             
             QueryFunction geneCount = new QueryFunction();

             q.addFrom(qcGene);
             q.addFrom(qcProtein);
             q.addFrom(qcOrganism);
             q.addFrom(qcProteinFeature);

             q.addToSelect(qfId);
             q.addToSelect(geneCount);
             q.addToSelect(qfName);

             ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);

         
                 // genes must be in bag
                 BagConstraint bc1 =
                     new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getOsb());
                 cs1.addConstraint(bc1);

             // get organisms
             organisms = BioUtil.getOrganisms(os, bag);

             // limit to organisms in the bag
             BagConstraint bc2 = new BagConstraint(qfOrganismName, ConstraintOp.IN, organisms);
             cs1.addConstraint(bc2);

             // gene is from organism
             QueryObjectReference qr1 = new QueryObjectReference(qcGene, "organism");
             ContainsConstraint cc1 
                                 = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qcOrganism);
             cs1.addConstraint(cc1);
             

             // gene.Proteins CONTAINS protein
             QueryCollectionReference qr2 = new QueryCollectionReference(qcGene, "proteins");
             ContainsConstraint cc2 =
                 new ContainsConstraint(qr2, ConstraintOp.CONTAINS, qcProtein);
             cs1.addConstraint(cc2);


             // protein.ProteinFeatures CONTAINS proteinFeature
             QueryCollectionReference qr3 
                 = new QueryCollectionReference(qcProtein, "proteinFeatures");
             ContainsConstraint cc3 =
                 new ContainsConstraint(qr3, ConstraintOp.CONTAINS, qcProteinFeature);
             cs1.addConstraint(cc3);

             SimpleConstraint sc = 
                 new SimpleConstraint(qfInterpro, ConstraintOp.MATCHES, new QueryValue("IPR%"));
             cs1.addConstraint(sc);
             
             q.setConstraint(cs1);
             q.addToGroupBy(qfId);
             q.addToGroupBy(qfName);
             
             sampleQuery = q;
             
             // construct population query
             q = new Query();
             q.setDistinct(false);

             q.addFrom(qcGene);
             q.addFrom(qcProtein);
             q.addFrom(qcOrganism);
             q.addFrom(qcProteinFeature);

             q.addToSelect(qfId);
             q.addToSelect(geneCount);

             ConstraintSet cs2 = new ConstraintSet(ConstraintOp.AND);
             cs2.addConstraint(cc1);
             cs2.addConstraint(cc2);
             cs2.addConstraint(cc3);
             cs2.addConstraint(bc2);
             cs2.addConstraint(sc);
             q.setConstraint(cs2);
             q.addToGroupBy(qfId);
             populationQuery = q;
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
         
}




