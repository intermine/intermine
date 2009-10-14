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

import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.bag.InterMineBag;
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
    public PathQuery generatePathQuery() {        
        // can't do this properly until #2184 if fixed
        PathQuery q = new PathQuery(os.getModel());
//        String viewStrings = "Gene.secondaryIdentifier";
//        String tiffin  = "Gene.upstreamIntergenicRegion.overlappingFeatures[TFBindingSite].motif.primaryIdentifier";
        String tiffin = "TFBindingSite.motif.primaryIdentifier";
//        q.setView(viewStrings);
        q.addView(tiffin);
//        q.addConstraint(bag.getType(),  Constraints.in(bag.getName()));
        q.addConstraint(tiffin, Constraints.eq
                        (key));
        q.addConstraint("TFBindingSite.dataSets.title", Constraints.eq(DATASET));
        q.setConstraintLogic("A and B");
        q.syncLogicExpression("and");
        q.setOrderBy(tiffin);
        return q;
    }
}

