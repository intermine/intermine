package org.intermine.web.config;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

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
    private LinkedHashMap fieldConfigMap = new LinkedHashMap();
    private ListOrderedSet longDisplayers = new ListOrderedSet();
    private ListOrderedSet graphDisplayers = new ListOrderedSet();
    private ListOrderedSet bagTableDisplayers = new ListOrderedSet();
    private Displayer tableDisplayer;
    private Map aspectDisplayers = new HashMap();

    /**
     * Set the fully-qualified class name for this Type
     * @param className the name of the Type
     */
    public void setClassName(String className) {
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
    public Collection getFieldConfigs() {
        return Collections.unmodifiableCollection(fieldConfigMap.values());
    }

    /**
     * Return a Map from FieldConfig.fieldName to FieldConfig objects.
     * @return the FieldConfig Map
     */
    public Map getFieldConfigMap() {
        return Collections.unmodifiableMap(fieldConfigMap);
    }

   /**
     * Add a long displayer for this Type
     * @param disp the Displayer to add
     */
    public void addLongDisplayer(Displayer disp) {
        longDisplayers.add(disp);
        String aspects[];
        if (StringUtils.isEmpty(disp.getAspects())) {
            aspects = new String[]{""};
        } else {
            aspects = StringUtils.split(disp.getAspects(), ',');
        }
        for (int i = 0; i < aspects.length; i++) {
            String aspect = aspects[i].trim();
            List displayers = (List) aspectDisplayers.get(aspect);
            if (displayers == null) {
                displayers = new ArrayList();
                aspectDisplayers.put(aspect, displayers);
            }
            displayers.add(disp);
        }
    }
    
    
    /**
     * Get the GraphDisplayers for thi type
     * @return the List of GraphDisplayers
     */
    public ListOrderedSet getGraphDisplayers() {
        return graphDisplayers;
    }
    
    /**
     * Get the BagTableDisplayers for thi type
     * @return the List of BagTableDisplayers
     */
    public ListOrderedSet getBagTableDisplayers() {
        return bagTableDisplayers;
    }

    /**
     * Add a GraphDisplayer to the List oof GraphDisplayers
     * for that type
     * @param gdisp a GraphDisplayer
     */
    public void addGraphDisplayer(GraphDisplayer gdisp) {
        graphDisplayers.add(gdisp);
    }

    /**
     * Add a BagTableDisplayer to the List oof BagTableDisplayers
     * for that type
     * @param btDisp a BagTableDisplayer
     */
    public void addBagTableDisplayer(BagTableDisplayer btDisp) {
        bagTableDisplayers.add(btDisp);
    }

    /**
     * Set the table displayer for this Type
     * @param disp the Displayer
     */
    public void setTableDisplayer(Displayer disp) {
        tableDisplayer = disp;
    }

    /**
     * Get the List of long Displayers
     * @return the List of long Displayers
     */
    public Set getLongDisplayers() {
        return Collections.unmodifiableSet(this.longDisplayers);
    }
    
    /**
     * Get the table Displayer
     * @return the table Displayer
     */
    public Displayer getTableDisplayer() {
        return tableDisplayer;
    }

    /**
     * @see Object#equals
     * @param obj the Object to compare with
     * @return true if this is equal to obj
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Type)) {
            return false;
        }

        Type typeObj = (Type) obj;
        
        return fieldConfigMap.equals(typeObj.fieldConfigMap)
            && longDisplayers.equals(typeObj.longDisplayers)
            && ObjectUtils.equals(tableDisplayer, typeObj.tableDisplayer);
    }

    /**
     * @see Object#hashCode
     * @return the hashCode for this Type object
     */
    public int hashCode() {
        int hash = fieldConfigMap.hashCode() + 3 * longDisplayers.hashCode();
        if (tableDisplayer != null) {
             hash += 5 * tableDisplayer.hashCode();
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
        Iterator iter = getFieldConfigs().iterator();
        while (iter.hasNext()) {
            sb.append(iter.next().toString());
        }
        sb.append("</fieldconfigs>");
        if (tableDisplayer != null) {
            sb.append(tableDisplayer.toString("tabledisplayer"));
        }
        sb.append("<longdisplayers>");
        iter = longDisplayers.iterator();
        while (iter.hasNext()) {
            sb.append(iter.next().toString());
        }
        sb.append("</longdisplayers>");
        sb.append("</class>");

        return sb.toString();
    }

    /**
     * @return return map from aspect name to list of long displayer
     */
    public Map getAspectDisplayers() {
        return aspectDisplayers;
    }

    /**
     * @param aspectDisplayers The aspectDisplayers to set.
     */
    public void setAspectDisplayers(Map aspectDisplayers) {
        this.aspectDisplayers = aspectDisplayers;
    }
}
