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
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.widget.GraphCategoryURLGenerator;
import org.jfree.data.category.CategoryDataset;

/**
 *
 * @author Julie Sullivan
 *
 */
public class BDGPinsituGraphURLGenerator implements GraphCategoryURLGenerator
{
    private String bagName;
    private static final String DATASET = "BDGP in situ data set";

    /**
     * Creates a GraphURLGenerator for the chart
     * @param bagName the bag name
     * @param organism not used
     */
    public BDGPinsituGraphURLGenerator(String bagName, String organism) {
        super();
        this.bagName = bagName;
    }

    /**
     * Creates a GraphURLGenerator for the chart
     * @param bagName the bag name
     */
    public BDGPinsituGraphURLGenerator(String bagName) {
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

        String seriesName = (String) dataset.getRowKey(series);
        seriesName = seriesName.toLowerCase();
        Boolean expressed = Boolean.FALSE;
        if ("expressed".equals(seriesName)) {
            expressed = Boolean.TRUE;
        }

        sb = new StringBuffer("queryForGraphAction.do?bagName=" + bagName);
        sb.append("&category=" + dataset.getColumnKey(category));
        sb.append("&series=" + expressed);
        sb.append("&urlGen=org.flymine.web.widget.BDGPinsituGraphURLGenerator");

        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery(ObjectStore os,
                                       InterMineBag bag,
                                       String category,
                                       String series) {

        Model model = os.getModel();
        PathQuery q = new PathQuery(model);

        q.addViews("Gene.primaryIdentifier", "Gene.secondaryIdentifier", "Gene.name",
                "Gene.organism.name", "Gene.mRNAExpressionResults.stageRange",
                "Gene.mRNAExpressionResults.expressed");

        // bag constraint
        q.addConstraint(Constraints.in("Gene", bag.getName()));

        // filter out flyFish
        q.addConstraint(Constraints.eq("Gene.mRNAExpressionResults.dataSet.name", DATASET));

        // stage (category)
        q.addConstraint(Constraints.eq("Gene.mRNAExpressionResults.stageRange",
                        category + " (BDGP in situ)"));

        // expressed (series)
        Boolean expressed = Boolean.valueOf("true".equals(series));
        q.addConstraint(Constraints.eq("Gene.mRNAExpressionResults.expressed",
                expressed.toString()));

        return q;
    }
}
