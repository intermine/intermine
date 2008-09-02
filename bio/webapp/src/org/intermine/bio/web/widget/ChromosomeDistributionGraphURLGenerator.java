package org.intermine.bio.web.widget;

/*
 * Copyright (C) 2002-2008 FlyMine
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
 * @author Julie Sullivan
 */
public class ChromosomeDistributionGraphURLGenerator implements GraphCategoryURLGenerator
{
    String bagName;
    String organism = null;

    /**
     * Creates a ChromosomeDistributionGraphURLGenerator for the chart
     * @param model
     * @param bagName name of bag for which to render this widget
     * @param organism constrain query by organism
     */
    public ChromosomeDistributionGraphURLGenerator(String bagName, String organism) {
        super();
        this.bagName = bagName;
        this.organism = organism;
    }

    /**
     * {@inheritDoc}
     * @see org.jfree.chart.urls.CategoryURLGenerator#generateURL(
     *      org.jfree.data.category.CategoryDataset,
     *      int, int)
     */
    public String generateURL(CategoryDataset dataset,
                              @SuppressWarnings("unused") int series,
                              int category) {
        StringBuffer sb = new StringBuffer("queryForGraphAction.do?bagName=" + bagName);
        sb.append("&category=" + dataset.getColumnKey(category));
        sb.append("&series=");
        sb.append("&urlGen=org.intermine.bio.web.widget.ChromosomeDistributionGraphURLGenerator");
        sb.append("&extraKey=" + organism);

        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery(ObjectStore os,
                                       InterMineBag imBag,
                                       @SuppressWarnings("unused") String category,
                                       @SuppressWarnings("unused") String series) {


        PathQuery q = new PathQuery(os.getModel());
        String bagType = imBag.getType();

        q.setView(bagType + ".secondaryIdentifier,"
                  + bagType + ".primaryIdentifier, "
                  + bagType + ".organism.name, "
                  + bagType + ".chromosome.primaryIdentifier,"
                  + bagType + ".chromosomeLocation.start,"
                  + bagType + ".chromosomeLocation.end,"
                  + bagType + ".chromosomeLocation.strand");

        q.addConstraint(bagType,  Constraints.in(imBag.getName()));
        q.addConstraint(bagType + ".chromosome.primaryIdentifier",  Constraints.eq(category));
        if (organism != null) {
            q.addConstraint(bagType + ".organism",  Constraints.lookup(organism));
            q.setConstraintLogic("A and B and C");
        } else {
            q.setConstraintLogic("A and B");
        }
        q.syncLogicExpression("and");
        q.setOrderBy(bagType + ".chromosomeLocation.start,"
                     + bagType + ".secondaryIdentifier,"
                     + bagType + ".primaryIdentifier,"
                     + bagType + ".organism.name, "
                     + bagType + ".chromosome.primaryIdentifier");
        return q;
    }
}
