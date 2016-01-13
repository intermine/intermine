package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

import org.intermine.objectstore.query.Results;

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
     * Get the Results object
     * @return the results Object
     */
    Results getResults();

    /**
     * This method is used to calculate the 'not analysed' total displayed on each widget
     * @return the total number of objects analysed in this widget
     */
    int getWidgetTotal();

    /**
     * Return the result table that represents the data from this widget.
     * @return The widget's data.
     */
    List<List<Object>> getResultTable();
}
