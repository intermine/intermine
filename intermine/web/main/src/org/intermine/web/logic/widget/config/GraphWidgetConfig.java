package org.intermine.web.logic.widget.config;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.widget.GraphWidget;

/**
 * Configuration object describing details of a graph displayer
 *
 * @author Xavier Watkins
 */
public class GraphWidgetConfig extends WidgetConfig
{
    private static final Logger LOG = Logger.getLogger(GraphWidgetConfig.class);
    private String domainLabel;
    private String rangeLabel;
    private String graphType;
    private String bagType;
    private String bagPath;
    private String categoryPath;
    private String seriesPath;
    private String seriesValues;
    private String seriesLabels;
    private String extraAttributeClass, externalLink, externalLinkLabel;
    private HttpSession session;
    private String editable;

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
               + rangeLabel + " dataSetLoader=\"" + getDataSetLoader()
               + " urlGen=\"" + getLink() + "\" />";
    }

    public String getBagType() {
        return bagType;
    }

    public void setBagType(String bagType) {
        this.bagType = bagType;
    }

    public String getBagPath() {
        return bagPath;
    }

    public void setBagPath(String bagPath) {
        this.bagPath = bagPath;
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
     * @return the extraAttributeClass
     */
    public String getExtraAttributeClass() {
        return extraAttributeClass;
    }

    /**
     * @param extraAttributeClass the extraAttributeClass to set
     */
    public void setExtraAttributeClass(String extraAttributeClass) {
        this.extraAttributeClass = extraAttributeClass;
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
        Collection<String> extraAttributes = new ArrayList<String>();
        Map<String, Collection<String>> returnMap = new HashMap<String, Collection<String>>();
        if (extraAttributeClass != null && extraAttributeClass.length() > 0) {
            try {
                Class<?> clazz = TypeUtil.instantiate(extraAttributeClass);
                Method extraAttributeMethod = clazz.getMethod("getExtraAttributes",
                            new Class[] {ObjectStore.class, InterMineBag.class});
                // invoking a static method the first argument is ignored
                extraAttributes = (Collection<String>) extraAttributeMethod.invoke(null, os, imBag);
            } catch (Exception e) {
                LOG.error(e.getMessage());
                return returnMap;
            }
        }
        if (extraAttributes.size() > 0) {
            returnMap.put("Organism", extraAttributes);
        }
        if (editable != null && "true".equals(editable)) {
            returnMap.put("Editable", new ArrayList<String>());
        }
        return returnMap;
    }

    /**
     * {@inheritDoc}
     */
    public String getExternalLink() {
        return externalLink;
    }

    /**
     * {@inheritDoc}
     */
    public void setExternalLink(String externalLink) {
        this.externalLink = externalLink;
    }

    /**
     * {@inheritDoc}
     */
    public String getExternalLinkLabel() {
        return externalLinkLabel;
    }

    /**
     * {@inheritDoc}
     */
    public void setExternalLinkLabel(String externalLinkLabel) {
        this.externalLinkLabel = externalLinkLabel;
    }

    /**
     * {@inheritDoc}
     */
    public GraphWidget getWidget(InterMineBag imBag, ObjectStore os,
                                 List<String> selectedExtraAttribute) {
        return new GraphWidget(this, imBag, os, selectedExtraAttribute.get(0));
    }

}
