package org.intermine.web.logic.widget.config;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.widget.GraphWidget;

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
    public static final String ACTUAL_EXPECTED_CRITERIA = "ActualExpectedCriteria";

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

    public String getBagType() {
        return bagType;
    }

    public void setBagType(String bagType) {
        this.bagType = bagType;
    }

    public String getListPath() {
        return listPath;
    }

    public void setListPath(String bagPath) {
        this.listPath = bagPath;
    }

    public boolean isListPathSet() {
        if (listPath != null && !"".equals(listPath)) {
            return true;
        }
        return false;
    }

    public String getCategoryPath() {
        return categoryPath;
    }

    public void setCategoryPath(String categoryPath) {
        this.categoryPath = categoryPath;
    }

    public String getSeriesPath() {
        return seriesPath;
    }

    public void setSeriesPath(String seriesPath) {
        this.seriesPath = seriesPath;
    }

    public boolean isActualExpectedCriteria() {
        if (this.seriesPath.contains(ACTUAL_EXPECTED_CRITERIA)) {
            return true;
        }
        return false;
    }

    public String getSeriesValues() {
        return seriesValues;
    }

    public void setSeriesValues(String seriesValues) {
        this.seriesValues = seriesValues;
    }

    public String getSeriesLabels() {
        return seriesLabels;
    }

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
    public GraphWidget getWidget(InterMineBag imBag, InterMineBag populationBag, ObjectStore os,
                                 List<String> selectedExtraAttribute) {
        return new GraphWidget(this, imBag, os, selectedExtraAttribute.get(0));
    }

}
