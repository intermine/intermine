package org.intermine.web.logic.widget;

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
import org.intermine.pathquery.PathQuery;
import org.jfree.chart.urls.CategoryURLGenerator;
import org.jfree.data.category.CategoryDataset;

/**
 * Methods used to construct the links used on the graph widgets
 * @author Julie Sullivan
 */
public interface GraphCategoryURLGenerator extends CategoryURLGenerator
{
    /**
     * builds the url used by the graph widgets.  Simply concatenates the variables to create
     * a URL.
     * @param dataset dataset represented by the entire graph
     * @param category specific category (eg employee type: manager, etc)
     * @param series specific series (eg part-time, full-time)
     * @return url that sends the user to the results page containing data represented by
     * the bar on the graph they clicked on
     */
    String generateURL(CategoryDataset dataset, int series, int category);

    /**
     * generates the path query
     * @param os object store
     * @param bag bag that this widget is displaying
     * @param series key to constrain the query
     * @param category category to constrain the query
     * @return PathQuery
     */
    PathQuery generatePathQuery(ObjectStore os, InterMineBag bag, String category, String series);
}
