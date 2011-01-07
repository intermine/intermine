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


import java.util.Arrays;

import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Constraints;
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

        String motifPath =
            "Gene.upstreamIntergenicRegion.overlappingFeatures.motif.primaryIdentifier";
        q.addViews("Gene.secondaryIdentifier", motifPath);

        q.addConstraints(Constraints.type("Gene.upstreamIntergenicRegion.overlappingFeatures",
            "TFBindingSite"));

        q.addConstraint(Constraints.eq("TFBindingSite.dataSets.name", DATASET));
        q.addConstraint(Constraints.in(bag.getType(),  bag.getName()));

        if (!showAll) {
            String[] keys = key.split(",");
            q.addConstraint(Constraints.oneOfValues(motifPath, Arrays.asList(keys)));
        }
        return q;
    }
}

