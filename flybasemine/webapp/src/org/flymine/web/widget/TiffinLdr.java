package org.flymine.web.widget;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;

import org.intermine.bio.web.logic.BioUtil;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.EnrichmentWidgetLdr;

import org.flymine.model.genomic.DataSet;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.IntergenicRegion;
import org.flymine.model.genomic.Motif;
import org.flymine.model.genomic.Organism;
import org.flymine.model.genomic.TFBindingSite;
/**
 * @author Julie Sullivan
 */
public class TiffinLdr extends EnrichmentWidgetLdr
{

    private Collection<String> organisms = new ArrayList<String>();
    private Collection<String> organismsLower = new ArrayList<String>();
    private InterMineBag bag;

    /**
     * @param bag list of objects for this widget
     * @param os object store
     * @param extraAttribute an extra attribute, probably organism
     */
     public TiffinLdr(InterMineBag bag, ObjectStore os, String extraAttribute) {
         this.bag = bag;
         organisms = BioUtil.getOrganisms(os, bag, false);
         for (String s : organisms) {
             organismsLower.add(s.toLowerCase());
         }
     }

     /**
      * {@inheritDoc}
      */
     public Query getQuery(String action, List<String> keys) {

         QueryClass qcGene = new QueryClass(Gene.class);
         QueryClass qcIntergenicRegion = new QueryClass(IntergenicRegion.class);
         QueryClass qcTFBindingSite = new QueryClass(TFBindingSite.class);
         QueryClass qcDataSet = new QueryClass(DataSet.class);
         QueryClass qcMotif = new QueryClass(Motif.class);
         QueryClass qcOrganism = new QueryClass(Organism.class);

         QueryField qfGeneId = new QueryField(qcGene, "id");
         QueryField qfPrimaryIdentifier = new QueryField(qcGene, "primaryIdentifier");
         QueryField qfOrganismNameMixedCase = new QueryField(qcOrganism, "name");
         QueryExpression qfOrganismName = new QueryExpression(QueryExpression.LOWER,
                 qfOrganismNameMixedCase);
         QueryField qfId = new QueryField(qcMotif, "primaryIdentifier");
         QueryField qfDataSet = new QueryField(qcDataSet, "title");

         ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

         if (keys != null) {
             cs.addConstraint(new BagConstraint(qfId, ConstraintOp.IN, keys));
         }

         if (!action.startsWith("population")) {
             cs.addConstraint(new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getOsb()));
         }

         cs.addConstraint(new BagConstraint(qfOrganismName, ConstraintOp.IN, organismsLower));

         QueryObjectReference qr1 = new QueryObjectReference(qcGene, "organism");
         cs.addConstraint(new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qcOrganism));

         QueryObjectReference qr2 =
             new QueryObjectReference(qcGene, "upstreamIntergenicRegion");
         cs.addConstraint(new ContainsConstraint(qr2, ConstraintOp.CONTAINS, qcIntergenicRegion));

         QueryCollectionReference qr3 =
             new QueryCollectionReference(qcIntergenicRegion, "overlappingFeatures");
         cs.addConstraint(new ContainsConstraint(qr3, ConstraintOp.CONTAINS, qcTFBindingSite));

         QueryCollectionReference qr4 =
             new QueryCollectionReference(qcTFBindingSite, "evidence");
         cs.addConstraint(new ContainsConstraint(qr4, ConstraintOp.CONTAINS, qcDataSet));

         QueryObjectReference  qr5 = new QueryObjectReference(qcTFBindingSite, "motif");
         cs.addConstraint(new ContainsConstraint(qr5, ConstraintOp.CONTAINS, qcMotif));

         cs.addConstraint(new SimpleConstraint(qfDataSet,
                                               ConstraintOp.EQUALS, new QueryValue("Tiffin")));
         Query subQ = new Query();
         subQ.setDistinct(true);

         subQ.addFrom(qcGene);
         subQ.addFrom(qcIntergenicRegion);
         subQ.addFrom(qcTFBindingSite);
         subQ.addFrom(qcDataSet);
         subQ.addFrom(qcMotif);
         subQ.addFrom(qcOrganism);

         subQ.addToSelect(qfGeneId);

         subQ.setConstraint(cs);

         if (keys != null) {
             return subQ;
         }

         QueryField outerQfId = new QueryField(subQ, qfId);
         QueryFunction geneCount = new QueryFunction();

         Query q = new Query();
         q.setDistinct(false);

         q.addFrom(subQ);


         if (action.equals("analysed") || action.equals("export")) {

                 q.addToSelect(qfId);
                 q.addToSelect(qfPrimaryIdentifier);
                 q.addToOrderBy(qfId);

         } else if (!action.endsWith("Total")) {
             q.addToSelect(outerQfId);
             q.addToSelect(geneCount);
             if (!action.startsWith("population")) {
                 q.addToSelect(outerQfId);
             }
             q.addToGroupBy(outerQfId);

         } else {
             q.addToSelect(geneCount);
         }
         return q;
     }
}



