package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import org.intermine.web.logic.bag.InterMineBag;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

/**
 *
 * @author Julie Sullivan
 */
public class EnrichmentWidgetForm extends ActionForm
{

    private String controller, title, link, description, filterLabel, label;
    private String errorCorrection, filter;
    private String filters;
    private InterMineBag bag;
    private String bagName;
    private String max;
    
    
    /**
     * Constructor
     */
    public EnrichmentWidgetForm() {
        initialise();
    }

    /**
     * Initialiser
     */
    public void initialise() {
        controller = "";
        title  = "";
        link = "";
        description = "";
        filterLabel = "";
        label = "";
        errorCorrection = "BenjaminiHochberg";
        filter = "";
        filters = "";
        bag = null;
        bagName = "";
        max = "0.10";
    }

    /**
     * @return the controller
     */
    public String getController() {
        return controller;
    }


    /**
     * controller is the class that renders the widget
     * @param controller the controller to set
     */
    public void setController(String controller) {
        this.controller = controller;
    }


    /**
     * description is informative text describing the widget
     * @return the description
     */
    public String getDescription() {
        return description;
    }


    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }


    /**
     * which method of errorcorrection the user chose.  Bonferroni, BenjaminiHochberg, or None
     * @return the errorCorrection
     */
    public String getErrorCorrection() {
        return errorCorrection;
    }


    /**
     * @param errorCorrection the errorCorrection to set
     */
    public void setErrorCorrection(String errorCorrection) {
        this.errorCorrection = errorCorrection;
    }


    /**
     * the label for the filter.  The filter is a dropdown that contrains the results in some way.
     * Most widgets don't have a filter.
     * @return the filterLabel
     */
    public String getFilterLabel() {
        return filterLabel;
    }


    /**
     * @param filterLabel the filterLabel to set
     */
    public void setFilterLabel(String filterLabel) {
        this.filterLabel = filterLabel;
    }

    /**
     * the bag that this widget is using
     * @return the bag
     */
    public InterMineBag getBag() {
        return bag;
    }


    /**
     * @param bag the bag to set
     */
    public void setBag(InterMineBag bag) {
        this.bag = bag;
    }

    /**
     * the link is the class that generates the url for each result.   
     * @return the link
     */
    public String getLink() {
        return link;
    }


    /**
     * @param link the link to set
     */
    public void setLink(String link) {
        this.link = link;
    }


    /**
     * maximum value to display.  user can update.  this is for display purposes only.
     * @return the max
     */
    public String getMax() {
        return max;
    }


    /**
     * @param max the max to set
     */
    public void setMax(String max) {
        this.max = max;
    }


    /**
     * title of the widget
     * @return the title
     */
    public String getTitle() {
        return title;
    }


    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the bagName
     */
    public String getBagName() {
        return bagName;
    }


    /**
     * name of bag that this widget is using.  we need both the bag and the bagname because 
     * sometimes we don't have the bag object.
     * @param bagName the bagName to set
     */
    public void setBagName(String bagName) {
        this.bagName = bagName;
    }

    /**
     * @return the bagType
     */
    public String getBagType() {
        return bag.getType();
    }

    /**
     * label for the results table.  appears as column header in results.
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }


    /**
     * filter values get used in the query to constrain the results in some way, and they can be
     * changed by the user.  
     * most widgets don't have a filter.
     * @return the filter
     */
    public String getFilter() {
        return filter;
    }


    /**
     * @param filter the filter to set
     */
    public void setFilter(String filter) {
        this.filter = filter;
    }


    /**
     * filter values get used in the query to constrain the results in some way, and they can be
     * changed by the user.  
     * most widgets don't have a filter.
     * @return the filters
     */
    public String getFilters() {
        return filters;
    }


    /**
     * @param filters the filters to set
     */
    public void setFilters(String filters) {
        this.filters = filters;
    }
   
    /**
     * {@inheritDoc}
     */
    @Override
    public void reset(@SuppressWarnings("unused") ActionMapping mapping, 
                      @SuppressWarnings("unused") HttpServletRequest request) {
        initialise();
    }
}
