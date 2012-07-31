package org.intermine.webservice.server.widget;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
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
    private String bagName;
    private List<String> extraAttributes = new ArrayList<String>();

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
    void setWidgetId(String widgetId) {
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
    void setExtraAttributes(List<String> extraAttributes) {
        this.extraAttributes = extraAttributes;
    }

    /**
     * Get the type of the bag
     * @return the className
     */
    public String getBagName() {
        return bagName;
    }

    /**
     * Set the type of list
     * @param className the className to set
     */
    void setBagName(String className) {
        this.bagName = className;
    }

}
