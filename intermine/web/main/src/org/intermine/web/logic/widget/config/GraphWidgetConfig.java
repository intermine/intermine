package org.intermine.web.logic.widget.config;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.intermine.objectstore.ObjectStore;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.GraphWidget;
import org.intermine.web.logic.widget.WidgetUtil;

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
    private static final int WIDTH = 430;
    private static final int HEIGHT = 350;
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
                Constructor<?> constr = clazz.getConstructor(new Class[]{});
                WidgetUtil widgetUtil = (WidgetUtil) constr.newInstance(new Object[] {});
                extraAttributes = widgetUtil.getExtraAttributes(os, imBag);
            } catch (Exception e) {
                throw new Exception(e.getMessage());
            }
        }
        if (extraAttributes.size() > 0) {
            returnMap.put("Organism", extraAttributes);
        }
        if (editable != null && editable.equals("true")) {
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
     * @return the wIDTH
     */
    public static int getWIDTH() {
        return WIDTH;
    }

    /**
     * @return the hEIGHT
     */
    public static int getHEIGHT() {
        return HEIGHT;
    }

    /**
     * {@inheritDoc}
     */
    public GraphWidget getWidget(InterMineBag imBag, ObjectStore os,
                                 List<String> selectedExtraAttribute) {
        return new GraphWidget(this, imBag, os, selectedExtraAttribute.get(0));
    }

}
