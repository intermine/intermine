package org.intermine.bio.web.widget;

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
public class ProteinDomainURLQuery implements WidgetURLQuery
{
    //private static final Logger LOG = Logger.getLogger(ProteinDomainURLQuery.class);
    private InterMineBag bag;
    private String key;
    private ObjectStore os;

    /**
     * @param key which protein domain the user clicked on
     * @param bag bag
     * @param os object store
     */
    public ProteinDomainURLQuery(ObjectStore os, InterMineBag bag, String key) {
        this.bag = bag;
        this.key = key;
        this.os = os;
    }

    /**
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery(boolean showAll) {
        PathQuery q = new PathQuery(os.getModel());
        String bagType = bag.getType();
        String prefix = ("Protein".equals(bagType) ? "Protein" : "Gene.proteins");
        if ("Gene".equals(bagType)) {
            q.addViews("Gene.secondaryIdentifier", "Gene.symbol");
        }
        q.addViews(prefix + ".primaryIdentifier",
                prefix + ".organism.name",
                prefix + ".proteinDomains.primaryIdentifier",
                prefix + ".proteinDomains.name");
        q.addConstraint(Constraints.in(bagType,  bag.getName()));
        if (!showAll) {
            String[] keys = key.split(",");
            q.addConstraint(Constraints.oneOfValues(prefix + ".proteinDomains.primaryIdentifier",
                    Arrays.asList(keys)));
        }
        return q;
    }
}
