package org.intermine.webservice.server.widget;

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

import org.intermine.webservice.server.WebServiceInput;

/**
 * WidgetsServiceInput is parameter object representing parameters
 * for the WidgetsService web service.
 * @author "Xavier Watkins"
 *
 */
public class WidgetsServiceInput extends WebServiceInput
{
    private String widgetId;
    private String className;
    private List<String> extraAttributes;
    private List<String> ids;

    /**
     * Get the name or id of the widget
     * @return the widgetName
     */
    public String getWidgetId() {
        return widgetId;
    }
    /**
     * Set the widget name or ID
     * @param widgetId the widgetId to set
     */
    public void setWidgetId(String widgetId) {
        this.widgetId = widgetId;
    }
    /**
     * Get the List of extra attributes
     * @return the extraAttributes
     */
    public List<String> getExtraAttributes() {
        return extraAttributes;
    }
    /**
     * Set the list of extra attributes
     * @param extraAttributes the extraAttributes to set
     */
    public void setExtraAttributes(List<String> extraAttributes) {
        this.extraAttributes = extraAttributes;
    }
    /**
     * Get the type of the bag
     * @return the className
     */
    public String getClassName() {
        return className;
    }
    /**
     * Set the type of list
     * @param className the className to set
     */
    public void setClassName(String className) {
        this.className = className;
    }
    /**
     * Get the list of ids
     * @return the ids
     */
    public List<String> getIds() {
        return ids;
    }
    /**
     * set the list of Ids
     * @param ids the ids to set
     */
    public void setIds(List<String> ids) {
        this.ids = ids;
    }

}
