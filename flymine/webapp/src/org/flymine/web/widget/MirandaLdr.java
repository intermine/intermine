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
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.MRNA;
import org.intermine.model.bio.MiRNATarget;
import org.intermine.objectstore.ObjectStore;
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
import org.intermine.web.logic.widget.EnrichmentWidgetLdr;

/**
 * @author Julie Sullivan
 */
public class MirandaLdr extends EnrichmentWidgetLdr
{

//    private static final Logger LOG = Logger.getLogger(MirandaLdr.class);
    private Collection<String> organisms = new ArrayList<String>();
    private Collection<String> organismsLower = new ArrayList<String>();
    private InterMineBag bag;

    /**
     * Create a new Loader.
     * @param bag list of objects for this widget
     * @param os object store
     * @param extraAttribute an extra attribute for this widget (if needed)
     */
    public MirandaLdr(InterMineBag bag, ObjectStore os, String extraAttribute) {
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

        QueryClass qcMiRNATarget = null;
        qcMiRNATarget = new QueryClass(MiRNATarget.class);

        QueryClass qcGene = new QueryClass(Gene.class);
        QueryClass qcMiR = new QueryClass(Gene.class);
        QueryClass qcTranscript = new QueryClass(MRNA.class);

        QueryField qfGeneIdentifier = new QueryField(qcGene, "symbol");
        QueryField qfGeneId = new QueryField(qcGene, "id");
        QueryField qfMiRIdentifier = new QueryField(qcMiR, "symbol");

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        if (keys != null) {
            cs.addConstraint(new BagConstraint(qfMiRIdentifier, ConstraintOp.IN, keys));
        }

        if (!action.startsWith("population")) {
            cs.addConstraint(new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getOsb()));
        }

        QueryCollectionReference r1 = new QueryCollectionReference(qcMiR, "miRNAtargets");
        cs.addConstraint(new ContainsConstraint(r1, ConstraintOp.CONTAINS, qcMiRNATarget));

        QueryObjectReference qcr1 = new QueryObjectReference(qcMiRNATarget, "target");
        cs.addConstraint(new ContainsConstraint(qcr1, ConstraintOp.CONTAINS, qcTranscript));

        QueryObjectReference qcr2 = new QueryObjectReference(qcTranscript, "gene");
        cs.addConstraint(new ContainsConstraint(qcr2, ConstraintOp.CONTAINS, qcGene));


        Query q = new Query();

        q.addFrom(qcMiRNATarget);
        q.addFrom(qcGene);
        q.addFrom(qcMiR);
        q.addFrom(qcTranscript);

        q.setConstraint(cs);

        // which columns to return when the user clicks on 'export'
        if ("export".equals(action)) {
            q.addToSelect(qfMiRIdentifier);
            q.addToSelect(qfGeneIdentifier);
            q.addToOrderBy(qfMiRIdentifier);

        // analysed query:  return the gene only
        } else if ("analysed".equals(action)) {
            q.addToSelect(qfGeneId);

        // total query:  only return the count of unique genes
        } else if (action.endsWith("Total")) {
            q.addToSelect(qfGeneId);

            Query subQ = q;
            q = new Query();
            q.addFrom(subQ);
            q.addToSelect(new QueryFunction());

        // enrichment queries
        } else {

            // subquery
            Query subQ = q;
            // used for count
            subQ.addToSelect(qfGeneId);
            // feature name
            subQ.addToSelect(qfMiRIdentifier);
            // needed so we can select this field in the parent query
            QueryField qfUniqueTargets = new QueryField(subQ, qfMiRIdentifier);

            q = new Query();
            q.setDistinct(false);
            q.addFrom(subQ);

            // add the unique-ified targets to select
            q.addToSelect(qfUniqueTargets);

            // gene count
            q.addToSelect(new QueryFunction());

            // if this is the sample query, it expects a third column
            if ("sample".equals(action)) {
                q.addToSelect(qfUniqueTargets);
            }

            // group by target
            q.addToGroupBy(qfUniqueTargets);
        }
        return q;
    }
}
