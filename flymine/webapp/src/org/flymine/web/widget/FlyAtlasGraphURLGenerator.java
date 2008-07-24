package org.flymine.web.widget;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.query.Constraints;
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
    public PathQuery generatePathQuery(ObjectStore os,
                                       InterMineBag bag,
                                       String category,
                                       String series) {

        Model model = os.getModel();
        PathQuery q = new PathQuery(model);

        q.setView("FlyAtlasResult.genes.secondaryIdentifier, "
                  + "FlyAtlasResult.genes.primaryIdentifier,"
                  + "FlyAtlasResult.genes.name,"
                  + "FlyAtlasResult.genes.organism.name,"
                  + "FlyAtlasResult.assays.name,"
                  + "FlyAtlasResult.enrichment,"
                  + "FlyAtlasResult.affyCall,FlyAtlasResult.mRNASignal,"
                  + "FlyAtlasResult.mRNASignalSEM,FlyAtlasResult.presentCall");

        q.addConstraint("FlyAtlasResult.genes",  Constraints.in(bag.getName()));
        q.addConstraint("FlyAtlasResult.affyCall",  Constraints.eq(series));
        q.addConstraint("FlyAtlasResult.assays.name",  Constraints.eq(category));

        Boolean sortAscending = (category.equalsIgnoreCase("up") ? Boolean.FALSE : Boolean.TRUE);

        q.setOrderBy("FlyAtlasResult.enrichment", sortAscending);
        q.addOrderBy("FlyAtlasResult.genes.secondaryIdentifier");

        q.setConstraintLogic("A and B and C");
        q.syncLogicExpression("and");

        return q;
    }

}
