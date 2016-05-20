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

import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.widget.config.WidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfigUtil;

/**
 * A representation of a query using a list tool. It combines the description of the
 * calculation/query to be performed (the config), the list over which to perform it (the bag),
 * the database to query within (the os) and the results of the query.
 *
 * TODO: split into <code>Request</code> and <code>Result</code> classes.
 *
 * @author "Xavier Watkins"
 * @author Daniela Butano
 */
public abstract class Widget
{
    protected WidgetConfig config;
    protected InterMineBag bag;
    protected String ids;
    protected ObjectStore os;
    protected int notAnalysed = 0;

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
    public int getNotAnalysed() {
        return notAnalysed;
    }

    /**
     * @param notAnalysed the number of objects not analysed in this widget
     */
    public void setNotAnalysed(int notAnalysed) {
        this.notAnalysed = notAnalysed;
    }

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
     * @return a list representing the rows containing a list of objects
     * @throws Exception if something goes wrong.
     */
    public abstract List<List<Object>> getResults() throws Exception;

    /**
     * Return the PathQuery generated dynamically by the attribute views in the configuration file
     * @return the pathquery
     */
    public abstract PathQuery getPathQuery();

    /**
     * Create a pathquery having a view composed by all items set in the view attribute
     * in the config file
     * @param os the object store
     * @param config the widget configuration
     * @return the path query created
     */
    protected PathQuery createPathQueryView(ObjectStore os, WidgetConfig config) {
        Model model = os.getModel();
        PathQuery q = new PathQuery(model);
        String[] views = config.getViews().split("\\s*,\\s*");
        String prefix = config.getStartClass() + ".";
        for (String view : views) {
            if (!view.startsWith(prefix)) {
                view = prefix + view;
            }
            view = WidgetConfigUtil.getPathWithoutSubClass(model, view);
            q.addView(view);
        }
        return q;
    }
}
