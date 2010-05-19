package org.flymine.web.widget;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathNode;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.widget.WidgetURLQuery;

/**
 * Builds a query to get all the genes (in bag) associated with specified go term.
 * @author Julie Sullivan
 */
public class TiffinURLQuery implements WidgetURLQuery
{

    private InterMineBag bag;
    private String key;
    private ObjectStore os;
    private static final String DATASET = "Tiffin";

    /**
     * @param key which bar the user clicked on
     * @param bag bag
     * @param os object store
     */
    public TiffinURLQuery(ObjectStore os, InterMineBag bag, String key) {
        this.bag = bag;
        this.key = key;
        this.os = os;
    }

    /**
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery(boolean showAll) {
        PathQuery q = new PathQuery(os.getModel());
        PathNode node = q.addNode("Gene.upstreamIntergenicRegion.overlappingFeatures");
        node.setType("TFBindingSite");
        String path = "Gene.upstreamIntergenicRegion.overlappingFeatures.motif.primaryIdentifier";
        q.setView("Gene.secondaryIdentifier," + path);
        q.addConstraint("TFBindingSite.dataSets.name", Constraints.eq(DATASET));
        q.addConstraint(bag.getType(),  Constraints.in(bag.getName()));
        if (!showAll) {
            q.addConstraint(path, Constraints.eq (key));
            q.setConstraintLogic("A and B and C");
        } else {
            q.setConstraintLogic("A and B");
        }
        q.syncLogicExpression("and");
        return q;
    }
}

