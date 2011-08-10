package org.flymine.web.widget;

/*
 * Copyright (C) 2002-2011 FlyMine
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

import org.intermine.api.profile.InterMineBag;
import org.intermine.bio.util.BioUtil;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.TFBindingSite;
import org.intermine.objectstore.ObjectStore;
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
import org.intermine.web.logic.widget.EnrichmentWidgetLdr;
/**
 * @author Julie Sullivan
 */
public class TiffinLdr extends EnrichmentWidgetLdr
{
//    private static final Logger LOG = Logger.getLogger(TiffinLdr.class);
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
        QueryClass qcIntergenicRegion = null;
        QueryClass qcTFBindingSite = new QueryClass(TFBindingSite.class);
        QueryClass qcDataSet = new QueryClass(DataSet.class);
        QueryClass qcMotif = null;
        QueryClass qcOrganism = new QueryClass(Organism.class);


        try {
            qcMotif = new QueryClass(Class.forName("Motif"));
            qcIntergenicRegion  = new QueryClass(Class.forName("IntergenicRegion"));
        } catch (ClassNotFoundException e) {
            return null;
        }

        QueryField qfGeneId = new QueryField(qcGene, "id");
        QueryField qfPrimaryIdentifier = new QueryField(qcGene, "primaryIdentifier");
        QueryField qfOrganismNameMixedCase = new QueryField(qcOrganism, "name");
        QueryExpression qfOrganismName = new QueryExpression(QueryExpression.LOWER,
                qfOrganismNameMixedCase);
        QueryField qfId = new QueryField(qcMotif, "primaryIdentifier");
        QueryField qfDataSet = new QueryField(qcDataSet, "name");

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
            new QueryCollectionReference(qcTFBindingSite, "dataSets");
        cs.addConstraint(new ContainsConstraint(qr4, ConstraintOp.CONTAINS, qcDataSet));

        QueryObjectReference  qr5 = new QueryObjectReference(qcTFBindingSite, "motif");
        cs.addConstraint(new ContainsConstraint(qr5, ConstraintOp.CONTAINS, qcMotif));

        cs.addConstraint(new SimpleConstraint(qfDataSet,
                ConstraintOp.EQUALS, new QueryValue("Tiffin")));
        Query q = new Query();

        q.setDistinct(true);

        q.addFrom(qcGene);
        q.addFrom(qcIntergenicRegion);
        q.addFrom(qcTFBindingSite);
        q.addFrom(qcMotif);
        q.addFrom(qcDataSet);
        q.addFrom(qcOrganism);

        q.setConstraint(cs);

        //         if (keys != null) {
            //             subQ.addToSelect(qfGeneId);
        //             return subQ;
        //         }


        if ("analysed".equals(action)) {
            q.addToSelect(qfGeneId);
        } else if ("export".equals(action)) {
            q.addToSelect(qfId);
            q.addToSelect(qfPrimaryIdentifier);
            q.addToOrderBy(qfId);
        } else if (action.endsWith("Total")) {
            q.addToSelect(qfGeneId);
            Query subQ = q;
            q = new Query();
            q.addFrom(subQ);
            q.addToSelect(new QueryFunction()); // gene count
        } else {
            q.addToSelect(qfId);
            q.addToSelect(new QueryFunction()); // gene count
            if ("sample".equals(action)) {
                q.addToSelect(qfId);
            }
            q.addToGroupBy(qfId);
        }
        return q;
    }
}
