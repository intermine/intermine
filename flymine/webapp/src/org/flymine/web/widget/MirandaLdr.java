package org.flymine.web.widget;

/*
 * Copyright (C) 2002-2009 FlyMine
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

import org.intermine.bio.web.logic.BioUtil;
import org.intermine.model.bio.Gene;
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
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.EnrichmentWidgetLdr;

/**
 * @author Julie Sullivan
 */
public class MirandaLdr extends EnrichmentWidgetLdr
{
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

        QueryClass qcMiRNATarget = new QueryClass(MiRNATarget.class);
        QueryClass qcGene = new QueryClass(Gene.class);

        QueryField qfPrimaryIdentifier = new QueryField(qcGene, "primaryIdentifier");
        QueryField qfGene = new QueryField(qcGene, "id");
        QueryField qfTarget = new QueryField(qcMiRNATarget, "primaryIdentifier");

        QueryFunction qfCount = new QueryFunction();

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        if (keys != null) {
            cs.addConstraint(new BagConstraint(qfTarget, ConstraintOp.IN, keys));
        }

        if (!action.startsWith("population")) {
            cs.addConstraint(new BagConstraint(qfGene, ConstraintOp.IN, bag.getOsb()));
        }

        QueryCollectionReference r1 = new QueryCollectionReference(qcGene, "miRNAtargets");
        cs.addConstraint(new ContainsConstraint(r1, ConstraintOp.CONTAINS, qcMiRNATarget));

        Query q = new Query();

        q.addFrom(qcMiRNATarget);
        q.addFrom(qcGene);
        q.setConstraint(cs);

        if (action.equals("export")) {
            q.addToSelect(qfTarget);
            q.addToSelect(qfPrimaryIdentifier);
            q.addToOrderBy(qfTarget);
            return q;
        } else if (action.equals("analysed")) {
            q.addToSelect(qfGene);
            return q;
        } else if (action.endsWith("Total")) {
            q.addToSelect(new QueryField(qcGene, "id"));
            
            Query subQ = q;
            q = new Query();
            q.addFrom(subQ);
            q.addToSelect(new QueryFunction());
                        
        } else {
            q.addToSelect(qfCount);
            if (action.equals("sample")) {
                q.addToSelect(qfTarget);
            }
            q.addToGroupBy(qfTarget);
         }
        return q;
    }
}




