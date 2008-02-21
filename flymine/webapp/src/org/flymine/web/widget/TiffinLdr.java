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
    private Query annotatedSampleQuery;
    private Query annotatedPopulationQuery;
    private Collection<String> organisms;
    private String externalLink, append;
    private ObjectStoreInterMineImpl os;
    private InterMineBag bag;
    
    /**
     * @param request The HTTP request we are processing
     */
     public TiffinLdr(HttpServletRequest request) {

         HttpSession session = request.getSession();
         Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
         ServletContext servletContext = session.getServletContext();
         os = (ObjectStoreInterMineImpl) servletContext.getAttribute(Constants.OBJECTSTORE);

         String bagName = request.getParameter("bagName");
         Map<String, InterMineBag> allBags =
             WebUtil.getAllBags(profile.getSavedBags(), servletContext);
         bag = allBags.get(bagName);
         organisms = BioUtil.getOrganisms(os, bag);

         annotatedSampleQuery = getQuery(false, true);
         annotatedPopulationQuery = getQuery(false, false);

     }

     /**
      * {@inheritDoc}
      */
     public Query getQuery(boolean calcTotal, boolean useBag) {

         Query q = new Query();
         q.setDistinct(false);
         QueryClass qcGene = new QueryClass(Gene.class);
         QueryClass qcIntergenicRegion = new QueryClass(IntergenicRegion.class);
         QueryClass qcTFBindingSite = new QueryClass(TFBindingSite.class);
         QueryClass qcDataSet = new QueryClass(DataSet.class);
         QueryClass qcMotif = new QueryClass(Motif.class);
         QueryClass qcOrganism = new QueryClass(Organism.class);

         QueryField qfGeneId = new QueryField(qcGene, "id");
         QueryField qfOrganismName = new QueryField(qcOrganism, "name");
         QueryField qfId = new QueryField(qcMotif, "primaryIdentifier");
         QueryField qfDataSet = new QueryField(qcDataSet, "title");

         QueryFunction geneCount = new QueryFunction();

         q.addFrom(qcGene);
         q.addFrom(qcIntergenicRegion);
         q.addFrom(qcTFBindingSite);
         q.addFrom(qcDataSet);
         q.addFrom(qcMotif);
         q.addFrom(qcOrganism);

         q.addToSelect(qfId);
         q.addToSelect(geneCount);
         if (useBag) {
             q.addToSelect(qfId);
         }

         ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
         if (useBag) {
             // genes must be in bag
             BagConstraint bc1 = new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getOsb());
             cs.addConstraint(bc1);
         }
         // get organisms
         

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

         q.setConstraint(cs);

         q.addToGroupBy(qfId);

         return q;
     }
     
     /**
      * {@inheritDoc}
       */
      public Query getAnnotatedSample() {
          return annotatedSampleQuery;
      }

      /**
      * {@inheritDoc}
       */
      public Query getAnnotatedPopulation() {
          return annotatedPopulationQuery;
      }

      /**
      * {@inheritDoc}
       */
      public Collection<String> getPopulationDescr() {
          return organisms;
      }
     
     /**
      * {@inheritDoc}
      */
     public String getExternalLink() {
         return externalLink;
     }

     /**
      * {@inheritDoc}
      */
     public String getAppendage() {
         return append;
     }

}



