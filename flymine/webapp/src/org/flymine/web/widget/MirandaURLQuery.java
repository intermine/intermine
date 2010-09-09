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
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.widget.WidgetURLQuery;

/**
 * @author Julie Sullivan
 */
public class MirandaURLQuery implements WidgetURLQuery
{
    private InterMineBag bag;
    private String key;
    private ObjectStore os;

    /**
     * @param key which record the user clicked on
     * @param bag bag
     * @param os object store
     */
    public MirandaURLQuery(ObjectStore os, InterMineBag bag, String key) {
        this.bag = bag;
        this.key = key;
        this.os = os;
    }

    /**
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery(boolean showAll) {
        PathQuery q = new PathQuery(os.getModel());
        q.addViews("Gene.symbol", "Gene.miRNAtargets.target.gene.symbol");
        q.addConstraint(Constraints.in("Gene.miRNAtargets.target.gene",  bag.getName()));
        if (!showAll) {
            q.addConstraint(Constraints.lookup("Gene", key, ""));
        }

        return q;
    }
}
