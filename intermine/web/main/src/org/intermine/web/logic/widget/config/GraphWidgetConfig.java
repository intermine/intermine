package org.intermine.web.logic.widget.config;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.widget.GraphWidget;
import org.intermine.web.logic.widget.WidgetOptions;

/**
 * Configuration object describing details of a graph displayer
 *
 * @author Xavier Watkins
 */
public class GraphWidgetConfig extends WidgetConfig
{
    private String domainLabel;
    private String rangeLabel;
    private String graphType;
    private String bagType;
    private String listPath;
    private String categoryPath;
    private String seriesPath;
    private String seriesValues;
    private String seriesLabels;
    private HttpSession session;
    private String editable;
    private static final String ACTUAL_EXPECTED_CRITERIA = "ActualExpectedCriteria";

    /**
     * Get the session
     * @return the session
     */
    public HttpSession getSession() {
        return session;
    }

    /**
     * @param session the session to set
     */
    public void setSession(HttpSession session) {
        this.session = session;
    }

    /**
     * Get the domainLabel
     * @return the domainLabel
     */
    public String getDomainLabel() {
        return domainLabel;
    }


    /**
     * Set the value of domainLabel
     * @param domainLabel a String
     */
    public void setDomainLabel(String domainLabel) {
        this.domainLabel = domainLabel;
    }


    /**
     * Get the value of rangeLabel
     * @return the rangeLabel
     */
    public String getRangeLabel() {
        return rangeLabel;
    }


    /**
     * Set the value of rangeLabel
     * @param rangeLabel a String
     */
    public void setRangeLabel(String rangeLabel) {
        this.rangeLabel = rangeLabel;
    }

    /**
     * @param graphType type of graph, e.g. BarChart, StackedBarChart
     */
    public void setGraphType(String graphType) {
        this.graphType = graphType;
    }


    /**
     * Get the type of this graph, e.g. BarChart, StackedBarChart
     * @return the type of this graph
     */
    public String getGraphType() {
        return graphType;
    }

    /**
     * Return an XML String of this Type object
     * @return a String version of this WebConfig object
     */
    public String toString() {
        return "< title=\"" + getTitle() + " domainLabel=\"" + domainLabel + " rangeLabel=\""
               + rangeLabel + " />";
    }

    /** @return the supported bag-types **/
    public String getBagType() {
        return bagType;
    }

    /** @param bagType The bag types this widget supports **/
    public void setBagType(String bagType) {
        this.bagType = bagType;
    }

    /** @return The list path **/
    public String getListPath() {
        return listPath;
    }

    /** @param bagPath the new value for the list path **/
    public void setListPath(String bagPath) {
        this.listPath = bagPath;
    }

    /** @return whether this the list path has a value. **/
    public boolean isListPathSet() {
        return !(listPath == null || "".equals(listPath));
    }

    /** @return the category path **/
    public String getCategoryPath() {
        return categoryPath;
    }

    /** @param categoryPath the path that defines the category we are charting **/
    public void setCategoryPath(String categoryPath) {
        this.categoryPath = categoryPath;
    }

    /** @return the series path **/
    public String getSeriesPath() {
        return seriesPath;
    }

    /** @param seriesPath the new value of the series path. **/
    public void setSeriesPath(String seriesPath) {
        this.seriesPath = seriesPath;
    }

    /** @return whether this chart widget compares actual and expected values. **/
    public boolean comparesActualToExpected() {
        return (hasSeries() && this.seriesPath.contains(ACTUAL_EXPECTED_CRITERIA));
    }

    /** @return whether the widget has a series. **/
    public boolean hasSeries() {
        return (this.seriesPath != null && !"".equals(this.seriesPath));
    }

    /** @return the series values **/
    public String getSeriesValues() {
        return seriesValues;
    }

    /** @param seriesValues the values of the series axis. **/
    public void setSeriesValues(String seriesValues) {
        this.seriesValues = seriesValues;
    }

    /** @return the labels for the series. **/
    public String getSeriesLabels() {
        return seriesLabels;
    }

    /** @param seriesLabels The labels for the series. **/
    public void setSeriesLabels(String seriesLabels) {
        this.seriesLabels = seriesLabels;
    }

    /**
     * @return the editable attribute
     */
    public String geteditable() {
        return editable;
    }

    /**
     * @param editable editable
     */
    public void seteditable(String editable) {
        this.editable = editable;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Collection<String>> getExtraAttributes(InterMineBag imBag, ObjectStore os)
        throws Exception {
        Map<String, Collection<String>> returnMap = new HashMap<String, Collection<String>>();
        return returnMap;
    }

    /**
     * {@inheritDoc}
     */
    public GraphWidget getWidget(
            InterMineBag imBag,
            InterMineBag populationBag,
            ObjectStore os,
            WidgetOptions options,
            String ids, String populationIds) {
        return new GraphWidget(this, imBag, os, options, ids);
    }

}
