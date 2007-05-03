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
 * Used instead of a CategoryDataset, so we can specify the categoryarray (the list of labels
 * that appear on the x-axis).  Useful when we want categories to show up, even when they have
 * no data points.
 * @author Julie Sullivan
 */
public class GraphDataSet
{

    CategoryDataset dataset;
    Object[] categoryArray;
    
    /**
     * 
     * @param dataset Dataset to be displayed on chart
     * @param categoryArray labels to be displayed on the x-axis
     */
    public GraphDataSet(CategoryDataset dataset, Object[] categoryArray) {
        this.dataset = dataset;
        this.categoryArray = categoryArray;
    }
 
    /**
     * 
     * @return the dataset
     */
    
    public CategoryDataset getDataSet() {
        return dataset;
    }
    
    /**
     * labels to be displayed on the x-axis
     * @return categoryArray
     */
    public Object[] getCategoryArray() {
        return categoryArray;
    }
    
}
