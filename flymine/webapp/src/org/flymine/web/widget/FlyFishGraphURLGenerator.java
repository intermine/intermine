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
public class FlyFishGraphURLGenerator implements GraphCategoryURLGenerator
{
    String bagName;
    private static final  String DATASET = "fly-Fish data set";

    /**
     * Creates a FlyFishGraphURLGenerator for the chart
     * @param bagName the bag name
     * @param organism not used
     */
    public FlyFishGraphURLGenerator(String bagName, String organism) {
        super();
        this.bagName = bagName;
    }

    /**
     * Creates a FlyFishGraphURLGenerator for the chart
     * @param bagName the bag name
     */
    public FlyFishGraphURLGenerator(String bagName) {
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
        sb.append("&urlGen=org.flymine.web.widget.FlyFishGraphURLGenerator");

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

        // filter out BDGP
        q.addConstraint(Constraints.eq("Gene.mRNAExpressionResults.dataSet.name", DATASET));

        // stage (category)
        q.addConstraint(Constraints.eq("Gene.mRNAExpressionResults.stageRange",
                        category + " (fly-FISH)"));

        // expressed (series)
        Boolean expressed = ("true".equals(series) ? Boolean.TRUE : Boolean.FALSE);
        q.addConstraint(Constraints.eq("Gene.mRNAExpressionResults.expressed",
                expressed.toString()));

        return q;
    }
}
