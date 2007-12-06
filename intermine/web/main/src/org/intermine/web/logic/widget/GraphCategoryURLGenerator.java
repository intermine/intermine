package org.intermine.web.logic.widget;

/* 
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.query.PathQuery;

import org.jfree.chart.urls.CategoryURLGenerator;
import org.jfree.data.category.CategoryDataset;

/**
 *
 * @author Julie Sullivan
 */
public interface GraphCategoryURLGenerator extends CategoryURLGenerator
{

    public String generateURL(CategoryDataset dataset, int series, int category);
    
    /**
     * 
     * @param os
     * @param bag
     * @param series
     * @param category
     * @return PathQuery
     */
    public PathQuery generatePathQuery(ObjectStore os,  
                                       InterMineBag bag,
                                       String series, 
                                       String category);
    
}
