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

import org.jfree.data.category.CategoryDataset;

/**
 *
 * @author Julie Sullivan
 */
public class GraphDataSet
{

    CategoryDataset dataset;
    Object[] categoryArray;
    
    /**
     * 
     * @param dataset
     * @param categoryArray
     */
    public GraphDataSet(CategoryDataset dataset, Object[] categoryArray) {
        this.dataset = dataset;
        this.categoryArray = categoryArray;
    }
 
    public CategoryDataset getDataSet() {
        return dataset;
    }
    
    public Object[] getCategoryArray() {
        return categoryArray;
    }
    
}
