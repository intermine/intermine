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

import org.intermine.objectstore.query.Results;
import org.jfree.data.general.Dataset;

/**
 * An Interface which defines a frame
 * in which data can be retrieved, organised and created
 * to then be passed to a JFreeChart for representation
 * @author Xavier Watkins
 *
 */
public interface DataSetLdr
{
    /**
     * Get the generated DataSet
     * @return the dataset
     */
    Dataset getDataSet();

    /**
     * Get the Results object
     * @return the results Object
     */
    Results getResults();

    /**
     * This method is used to calculate the 'not analysed' total displayed on each widget
     * @return the total number of objects analysed in this widget
     */
    int getWidgetTotal();
}
