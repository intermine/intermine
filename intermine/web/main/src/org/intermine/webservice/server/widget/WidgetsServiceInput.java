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
 * @author Daniela Butano
 *
 */
public class WidgetsServiceInput extends WebServiceInput
{
    private String widgetId;
    private String bagName;
    private String populationBagName;
    private boolean savePopulation = false;
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
     * @return the bagName
     */
    public String getBagName() {
        return bagName;
    }

    /**
     * Set the bag's name
     * @param bagName the bagName to set
     */
    void setBagName(String bagName) {
        this.bagName = bagName;
    }

    /**
     * Get the bag's name for reference population
     * @return the bagName
     */
    public String getPopulationBagName() {
        return populationBagName;
    }

    /**
     * Set the bag's name for reference population
     * @param populationBagName the bagName to set
     */
    public void setPopulationBagName(String populationBagName) {
        this.populationBagName = populationBagName;
    }

    public boolean isSavePopulation() {
        return savePopulation;
    }

    public void setSavePopulation(boolean savePopulation) {
        this.savePopulation = savePopulation;
    }

}
