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

import java.util.List;

import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.widget.config.WidgetConfig;


/**
 * @author "Xavier Watkins"
 */
public abstract class Widget
{
    protected WidgetConfig config;

    /**
     * The constructor
     * @param config the WidgetConfig
     */
    public Widget(WidgetConfig config) {
        this.config = config;
    }

    /**
     * Process the data and create the widget
     *
     * @throws Exception if one of the classes in the widget isn't found
     */
    public abstract void process() throws Exception;

    /**
     * @return the number of objects not analysed in this widget
     */
    public abstract int getNotAnalysed();

    /**
     * @param notAnalysed the number of objects not analysed in this widget
     */
    public abstract void setNotAnalysed(int notAnalysed);

    /**
     *
     * @param selected the list of checked items from the form
     * @return the checked items in export format
     * @throws Exception something has gone wrong. oh no.
     */
    public abstract List<List<String>> getExportResults(String[]selected) throws Exception;

    /**
     * @return the hasResults
     */
    public abstract boolean getHasResults();

    /**
     * checks if elem is in bag
     * @return true if elem is in bag
     */
    public abstract List<String> getElementInList();

    /**
     * Get the ID of the corresponding WidgetConfig
     * @return the WidgetConfig ID
     */
    public String getConfigId() {
        return config.getId();
    }

    /**
     * Get the widget title
     * @return the title
     */
    public String getTitle() {
        return config.getTitle();
    }

    /**
     * Return the result that represents the data from this widget.
     * Each row is represented as a list of Object
     * @return a list representing the rows conatining a list of objects
     * @throws Exception
     */
    public abstract List<List<Object>> getResults() throws Exception;

    /**
     * Return the PathQuery generated dinamically by the attribute views in the config file
     * @return the pathquery
     */
    public abstract PathQuery getPathQuery();
}
