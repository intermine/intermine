package org.intermine.web.config;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.Set;
import java.util.Collections;

import org.apache.commons.collections.set.ListOrderedSet;

/**
 * Configuration object for displaying a class
 *
 * @author Andrew Varley
 */
public class Type
{
    // if fieldName is null it's ignored and the webapp will use the default renderer
    private String fieldName;
    private String className;
    private ListOrderedSet fieldConfigs = new ListOrderedSet();
    private ListOrderedSet longDisplayers = new ListOrderedSet();
    private Displayer tableDisplayer;

    /**
     * Set the fully-qualified class name for this Type
     *
     * @param className the name of the Type
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Get the class name
     *
     * @return the name
     */
    public String getClassName() {
        return this.className;
    }

    /**
     * Add a FieldConfig for this Type
     *
     * @param df the FieldConfig to add
     */
    public void addFieldConfig(FieldConfig df) {
        fieldConfigs.add(df);
    }

    /**
     * Get the List of FieldConfig objects
     *
     * @return the List of FieldConfig objects
     */
    public Set getFieldConfigs() {
        return Collections.unmodifiableSet(this.fieldConfigs);
    }

   /**
     * Add a long displayer for this Type
     *
     * @param disp the Displayer to add
     */
    public void addLongDisplayer(Displayer disp) {
        longDisplayers.add(disp);
    }
    
    /**
     * Set the table displayer for this Type
     *
     * @param disp the Displayer
     */
    public void setTableDisplayer(Displayer disp) {
        tableDisplayer = disp;
    }

    /**
     * Get the List of long Displayers
     *
     * @return the List of long Displayers
     */
    public Set getLongDisplayers() {
        return Collections.unmodifiableSet(this.longDisplayers);
    }
    
    /**
     * Get the table Displayer
     *
     * @return the table Displayer
     */
    public Displayer getTableDisplayer() {
        return tableDisplayer;
    }

    /**
     * @see Object#equals
     *
     * @param obj the Object to compare with
     * @return true if this is equal to obj
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Type)) {
            return false;
        }

        Type typeObj = (Type) obj;
        
        return fieldConfigs.equals(typeObj.fieldConfigs)
            && longDisplayers.equals(typeObj.longDisplayers)
            && tableDisplayer.equals(typeObj.tableDisplayer);
    }

    /**
     * @see Object#hashCode
     *
     * @return the hashCode for this Type object
     */
    public int hashCode() {
        return fieldConfigs.hashCode() + 3 * longDisplayers.hashCode() + 5 * tableDisplayer.hashCode();
    }

    /**
     * Return an XML String of this Type object
     *
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
        Iterator iter = fieldConfigs.iterator();
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
}
