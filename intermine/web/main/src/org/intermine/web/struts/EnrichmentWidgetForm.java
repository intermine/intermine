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
    private Double max;

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
        errorCorrection = "Benjamini and Hochberg";
        filter = "";
        filters = "";
        bag = null;
        bagName = "";
        max = new Double(0.10);
    }

    /**
     * @return the controller
     */
    public String getController() {
        return controller;
    }


    /**
     * @param controller the controller to set
     */
    public void setController(String controller) {
        this.controller = controller;
    }


    /**
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
     * @return the filterLabel
     */
    public String getFilterLabel() {
        return filterLabel;
    }


    /**
       /**


       * @param filterLabel the filterLabel to set
       */
    public void setFilterLabel(String filterLabel) {
        this.filterLabel = filterLabel;
    }

    /**
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
     * @return the max
     */
    public Double getMax() {
        return max;
    }


    /**
     * @param max the max to set
     */
    public void setMax(Double max) {
        this.max = max;
    }


    /**
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
        public void reset(ActionMapping mapping, HttpServletRequest request) {

        super.reset(mapping, request);
        initialise();
    }
}
