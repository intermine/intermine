package org.intermine.web.logic.config;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.commons.lang.ObjectUtils;
import org.intermine.web.logic.widget.config.WidgetConfig;

/**
 * Configuration object for displaying a class
 *
 * @author Andrew Varley
 * @author Thomas Riley
 */
public class Type
{
    // if fieldName is null it's ignored and the webapp will use the default renderer
    private String fieldName;
    private String className;
    private LinkedHashMap<String, FieldConfig> fieldConfigMap =
        new LinkedHashMap<String, FieldConfig>();
    private ListOrderedSet bagDisplayers = new ListOrderedSet();
    private LinkedList<WidgetConfig> widgets = new LinkedList<WidgetConfig>();
    private Displayer tableDisplayer;
    private Map<String, List<Displayer>> aspectDisplayers = new HashMap<String, List<Displayer>>();

    /** @var inline lists attached to the object type */
    private LinkedList<InlineList> inlineLists = new LinkedList<InlineList>();

    /** @var header configuration having paths to titles to show */
    private HeaderConfigTitle headerConfigTitle;
    private HeaderConfigLink headerConfigLink;

    /**
     * Set the unqualified class name for this Type (from fully-qualified)
     * @param className the name of the Type
     */
    public void setClassName(String className) {
        //this.className = TypeUtil.unqualifiedName(className);
        this.className = className;
    }

    /**
     * Get the class name
     * @return the name
     */
    public String getClassName() {
        return this.className;
    }

    /**
     * Add a FieldConfig for this Type
     * @param df the FieldConfig to add
     */
    public void addFieldConfig(FieldConfig df) {
        fieldConfigMap.put(df.getFieldExpr(), df);
    }

    /**
     * Get the List of FieldConfig objects
     * @return the List of FieldConfig objects
     */
    public Collection<FieldConfig> getFieldConfigs() {
        return Collections.unmodifiableCollection(fieldConfigMap.values());
    }

    /**
     * Return a Map from FieldConfig.fieldName to FieldConfig objects.
     * @return the FieldConfig Map
     */
    public Map<String, FieldConfig> getFieldConfigMap() {
        return Collections.unmodifiableMap(fieldConfigMap);
    }

    /**
     * Add a header configuration, used from WebConfig
     * @param headerConfig lalala
     */
    public void addHeaderConfigTitle(HeaderConfigTitle headerConfig) {
        this.headerConfigTitle = headerConfig;
    }

    /**
     *
     * @return HeaderConfigTitle
     */
    public HeaderConfigTitle getHeaderConfigTitle() {
        return this.headerConfigTitle;
    }

    /**
     * Add a header configuration, used from WebConfig
     * @param headerConfig lalala
     */
    public void addHeaderConfigLink(HeaderConfigLink headerConfig) {
        this.headerConfigLink = headerConfig;
    }

    /**
    *
    * @return HeaderConfigTitle
    */
    public HeaderConfigLink getHeaderConfigLink() {
        return this.headerConfigLink;
    }

    /**
     * Add an InlineList for this object, used from WebConfig
     * @param list lalala
     */
    public void addInlineList(InlineList list) {
        inlineLists.add(list);
    }

    /**
     * Add a bag displayer for this Type
     * @param disp the Displayer to add
     */
    public void addBagDisplayer(Displayer disp) {
        bagDisplayers.add(disp);
    }

    /**
     * @return the widgets
     */
    public LinkedList<WidgetConfig> getWidgets() {
        return widgets;
    }

    /**
     * @param widgets the widgets to set
     */
    public void setWidgets(LinkedList widgets) {
        this.widgets = widgets;
    }

    /**
     * Add a widget to the Type
     * @param widget a widget
     */
    public synchronized void addWidget(WidgetConfig widget) {
        widgets.add(widget);
    }

    /**
     * Set the table displayer for this Type
     * @param disp the Displayer
     */
    public void setTableDisplayer(Displayer disp) {
        tableDisplayer = disp;
    }

    /**
     *
     * @return inline lists
     */
    public List<InlineList> getInlineLists() {
        return inlineLists;
    }

    /**
     * Get the List of bag Displayers
     * @return the List of bag Displayers
     */
    public Set getBagDisplayers() {
        return Collections.unmodifiableSet(this.bagDisplayers);
    }

    /**
     * Get the table Displayer
     * @return the table Displayer
     */
    public Displayer getTableDisplayer() {
        return tableDisplayer;
    }

    /**
     * {@inheritDoc}
     * @param obj the Object to compare with
     * @return true if this is equal to obj
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Type)) {
            return false;
        }
        Type typeObj = (Type) obj;
        return fieldConfigMap.equals(typeObj.fieldConfigMap)
            && ObjectUtils.equals(bagDisplayers, typeObj.bagDisplayers)
            && ObjectUtils.equals(tableDisplayer, typeObj.tableDisplayer)
            && ObjectUtils.equals(widgets, typeObj.widgets)
            && ObjectUtils.equals(aspectDisplayers, typeObj.aspectDisplayers)
            && ObjectUtils.equals(headerConfigTitle, typeObj.headerConfigTitle)
            && ObjectUtils.equals(headerConfigLink, typeObj.headerConfigLink)
            && ObjectUtils.equals(inlineLists, typeObj.inlineLists);
    }

    /**
     * {@inheritDoc}
     * @return the hashCode for this Type object
     */
    public int hashCode() {
        int hash = fieldConfigMap.hashCode();
        if (inlineLists != null) {
            hash += 3 * inlineLists.hashCode();
        }
        if (tableDisplayer != null) {
            hash += 5 * tableDisplayer.hashCode();
        }
        if (widgets != null) {
            hash += 7 * widgets.hashCode();
        }
        if (aspectDisplayers != null) {
            hash += 11 * aspectDisplayers.hashCode();
        }
        if (headerConfigTitle != null) {
            hash += 13 * headerConfigTitle.hashCode();
        }
        if (headerConfigLink != null) {
            hash += 17 * headerConfigLink.hashCode();
        }
        if (inlineLists != null) {
            hash += 19 * inlineLists.hashCode();
        }

        return hash;
    }

    /**
     * Return an XML String of this Type object
     * @return a String version of this WebConfig object
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("<class className=\"" + className + "\"");
        if (fieldName != null) {
            sb.append(" fieldName=\"" + fieldName + "\"");
        }
        sb.append(">");
        sb.append("<fieldconfigs>");
        for (FieldConfig fc : getFieldConfigs()) {
            sb.append(fc.toString());
        }
        sb.append("</fieldconfigs>");
        if (tableDisplayer != null) {
            sb.append(tableDisplayer.toString("tabledisplayer"));
        }
        sb.append("</class>");

        return sb.toString();
    }

    /**
     * @return return map from aspect name to list of long displayer
     */
    public Map<String, List<Displayer>> getAspectDisplayers() {
        return aspectDisplayers;
    }

    /**
     * @param aspectDisplayers The aspectDisplayers to set.
     */
    public void setAspectDisplayers(Map<String, List<Displayer>> aspectDisplayers) {
        this.aspectDisplayers = aspectDisplayers;
    }

    /**
     * @see unused getter for WebConfig to work
     * @return null
     */
    public String getMainTitles() {
        return null;
    }
    /**
     * @see unused getter for WebConfig to work
     * @return null
     */
    public String getSubTitles() {
        return null;
    }

}
