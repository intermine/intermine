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

import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.widget.GraphCategoryURLGenerator;
import org.jfree.data.category.CategoryDataset;
/**
 *
 * @author Xavier Watkins
 *
 */
public class FlyAtlasGraphURLGenerator implements GraphCategoryURLGenerator
{
    String bagName;

    /**
     * Creates a FlyAtlasGraphURLGenerator for the chart
     * @param bagName the bag name
     * @param extra unused
     */
    public FlyAtlasGraphURLGenerator(String bagName, String extra) {
        super();
        this.bagName = bagName;
    }

    /**
     * Creates a FlyAtlasGraphURLGenerator for the chart
     * @param bagName the bag name
     */
    public FlyAtlasGraphURLGenerator(String bagName) {
        super();
        this.bagName = bagName;
    }

    /**
     * {@inheritDoc}
     * @see org.jfree.chart.urls.CategoryURLGenerator#generateURL(
     *      org.jfree.data.category.CategoryDataset,
     *      int, int)
     */
    public String generateURL(CategoryDataset dataset, int series, int category) {
        StringBuffer sb = new StringBuffer("queryForGraphAction.do?bagName=" + bagName);
        sb.append("&category=" + dataset.getColumnKey(category));
        sb.append("&series=" + dataset.getRowKey(series));
        sb.append("&urlGen=org.flymine.web.widget.FlyAtlasGraphURLGenerator");
        return sb.toString();
    }


    /**
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery(ObjectStore os, InterMineBag bag,
                                       String category, String series) {
        String bagType = bag.getType();

        // initialise our query
        PathQuery q = new PathQuery(os.getModel());

        if ("ProbeSet".equals(bagType)) {
            q.addConstraint(Constraints.in("FlyAtlasResult.genes.probeSets", bag.getName()));
            q.addView("FlyAtlasResult.genes.probeSets.primaryIdentifier");
        } else {
            q.addConstraint(Constraints.in("FlyAtlasResult.genes", bag.getName()));
        }

        // add columns to the output
        q.addViews("FlyAtlasResult.genes.secondaryIdentifier",
                  "FlyAtlasResult.genes.symbol",
                  "FlyAtlasResult.genes.name",
                  "FlyAtlasResult.genes.organism.name",
                  "FlyAtlasResult.tissue.name",
                  "FlyAtlasResult.enrichment",
                  "FlyAtlasResult.affyCall",
                  "FlyAtlasResult.mRNASignal",
                  "FlyAtlasResult.mRNASignalSEM",
                  "FlyAtlasResult.presentCall");

        // sort based on whether up or down regulated
        OrderDirection sortOrder = (category.equalsIgnoreCase("up") ? OrderDirection.ASC
                                                               : OrderDirection.DESC);
        // affyCall (up or down) value has to match what the user clicked on
        q.addConstraint(Constraints.eq("FlyAtlasResult.affyCall", series));

        // assay (tissue) has to match what the user clicked on
        q.addConstraint(Constraints.eq("FlyAtlasResult.tissue.name", category));

        q.addOrderBy("FlyAtlasResult.enrichment", sortOrder);
        q.addOrderBy("FlyAtlasResult.genes.secondaryIdentifier", OrderDirection.ASC);

        return q;
    }
}
