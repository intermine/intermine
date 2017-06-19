package org.intermine.webservice.server.widget;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.web.logic.widget.EnrichmentOptions;

/**
 * WidgetsServiceInput is parameter object representing parameters
 * for the WidgetsService web service.
 *
 * This class is read-only, containing only getters.
 *
 * @author "Xavier Watkins"
 * @author Daniela Butano
 *
 */
public class WidgetsServiceInput implements EnrichmentOptions
{
    protected String widgetId = null;
    protected String bagName = null;
    protected String populationBagName = null;
    protected boolean savePopulation = false;
    protected String filter = null;
    protected double maxP = 0.05d;
    protected String correction = null;
    protected String extraAttribute = null;
    protected String ids = null;
    protected String populationIds = null;


    /**
     * Get the name or id of the widget
     * @return the widgetName
     */
    public String getWidgetId() {
        return widgetId;
    }

    /**
     * Get the name of the bag
     * @return the bagName
     */
    public String getBagName() {
        return bagName;
    }

    /**
     * Get the bag's name for reference population
     * @return the bagName
     */
    public String getPopulationBagName() {
        return populationBagName;
    }

    /** @return whether we should save the population list. **/
    public boolean shouldSavePopulation() {
        return savePopulation;
    }

    /** @return list of intermine object IDs to analyse **/
    public String getIds() {
        return ids;
    }

    /** @return list of intermine object IDs to analyse instead of populationBagName **/
    public String getPopulationIds() {
        return populationIds;
    }

    @Override
    public String getFilter() {
        return filter;
    }

    @Override
    public double getMaxPValue() {
        return maxP;
    }

    @Override
    public String getCorrection() {
        return correction;
    }

    /** @return any other extra attribute **/
    public String getExtraAttribute() {
        return extraAttribute;
    }

    @Override
    public String getExtraCorrectionCoefficient() {
        return extraAttribute;
    }

    @Override
    public String toString() {
        return String.format(
            "WidgetServiceInput("
                + "widgetId = %s, bagName = %s,"
                + " maxP = %s, correction = %s,"
                + " pop = %s, savePop = %s, filter = %s,"
                + " extra = %s)",
                widgetId, bagName,
                maxP, correction,
                populationBagName, savePopulation, filter,
                extraAttribute, ids, populationIds);
    }

    /**
     * Class to build Inputs for the Widget Service. This class contains all the
     * setters.
     * @author Alex Kalderimis
     *
     */
    public static class Builder extends WidgetsServiceInput
    {
        /**
         * Set the widget name or ID
         * @param widgetId the widgetId to set
         */
        void setWidgetId(String widgetId) {
            this.widgetId = widgetId;
        }

        /**
         * Set the bag's name
         * @param bagName the bagName to set
         */
        void setBagName(String bagName) {
            this.bagName = bagName;
        }

        /**
         * Set the bag's name for reference population
         * @param populationBagName the bagName to set
         */
        public void setPopulationBagName(String populationBagName) {
            this.populationBagName = populationBagName;
        }

        /** @param savePopulation whether we should save the population list. **/
        public void setSavePopulation(boolean savePopulation) {
            this.savePopulation = savePopulation;
        }

        /** @param correction the correction algorithm to use. **/
        public void setCorrection(String correction) {
            this.correction = correction;
        }

        /** @param maxp The maximum acceptable p-value **/
        public void setMaxP(double maxp) {
            this.maxP = maxp;
        }

        /** @param filter the filter for this request **/
        public void setFilter(String filter) {
            this.filter = filter;
        }

        /** @param ids list of intermine object IDs **/
        public void setIds(String ids) {
            this.ids = ids;
        }

        /** @param populationIds list of intermine object IDs to use isntead of populationBagName
        **/
        public void setPopulationIds(String populationIds) {
            this.populationIds = populationIds;
        }

        /** @param extraAttribute the extra attribute for this request. **/
        public void setExtraAttribute(String extraAttribute) {
            this.extraAttribute = extraAttribute;
        }
    }

}
