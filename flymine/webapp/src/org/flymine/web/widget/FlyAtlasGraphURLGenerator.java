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
    public FlyAtlasGraphURLGenerator(String bagName, @SuppressWarnings("unused") String extra) {
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

        // initialise our query
        PathQuery q = new PathQuery(os.getModel());

        // add columns to the output
        q.setView("FlyAtlasResult.genes.secondaryIdentifier, "
                  + "FlyAtlasResult.genes.primaryIdentifier,"
                  + "FlyAtlasResult.genes.name,"
                  + "FlyAtlasResult.genes.organism.name,"
                  + "FlyAtlasResult.assays.name,"
                  + "FlyAtlasResult.enrichment,"
                  + "FlyAtlasResult.affyCall,FlyAtlasResult.mRNASignal,"
                  + "FlyAtlasResult.mRNASignalSEM,FlyAtlasResult.presentCall");

        // all results have to be in list
        q.addConstraint("FlyAtlasResult.genes", Constraints.in(bag.getName()));

        // sort based on whether up or down regulated
        String sortDirection = (category.equalsIgnoreCase("up") ? PathQuery.ASCENDING
                                                               : PathQuery.DESCENDING);
        // affyCall (up or down) value has to match what the user clicked on
        q.addConstraint("FlyAtlasResult.affyCall", Constraints.eq(series));

        // assay (tissue) has to match what the user clicked on
        q.addConstraint("FlyAtlasResult.assays.name", Constraints.eq(category));

        q.setOrderBy("FlyAtlasResult.enrichment", sortDirection);
        q.addOrderBy("FlyAtlasResult.genes.secondaryIdentifier");

        // set constraint logic
        q.setConstraintLogic("A and B and C");
        q.syncLogicExpression("and");
        return q;
    }
}
