package org.intermine.web;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.intermine.util.TypeUtil;

/**
 * Bean to represent a QueryClass during Query construction in the webapp
 * @author Mark Woodbridge
 */
public class DisplayQueryClass
{
    String type;
    List constraintNames = new ArrayList();
    Map fieldNames = new HashMap();
    Map fieldOps = new HashMap();
    Map fieldValues = new HashMap();

    /**
     * Gets the value of type
     *
     * @return the value of type
     */
    public String getType()  {
        return this.type;
    }

    /**
     * Sets the value of type
     *
     * @param type Value to assign to this.type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the unqualified type
     * 
     * @return the unqualified type
     */
    public String getUnqualifiedType() {
        return TypeUtil.unqualifiedName(type);
    }

    /**
     * Gets the value of constraintNames
     *
     * @return the value of constraintNames
     */
    public List getConstraintNames()  {
        return this.constraintNames;
    }

    /**
     * Sets the value of constraintNames
     *
     * @param argConstraintNames Value to assign to this.constraintNames
     */
    public void setConstraintNames(List argConstraintNames) {
        this.constraintNames = argConstraintNames;
    }

    /**
     * Gets the value of fieldNames
     *
     * @return the value of fieldNames
     */
    public Map getFieldNames()  {
        return this.fieldNames;
    }

    /**
     * Sets the value of fieldNames
     *
     * @param argFieldNames Value to assign to this.fieldNames
     */
    public void setFieldNames(Map argFieldNames) {
        this.fieldNames = argFieldNames;
    }

    /**
     * Set a value in the fieldNames map
     * @param key the key
     * @param value the value
     */
    public void setFieldName(String key, Object value) {
        fieldNames.put(key, value);
    }

    /**
     * Get a value from the fieldNames map
     * @param key the key
     * @return the value
     */
    public Object getFieldName(String key) {
        return fieldNames.get(key);
    }

    /**
     * Gets the value of fieldOps
     *
     * @return the value of fieldOps
     */
    public Map getFieldOps()  {
        return this.fieldOps;
    }

    /**
     * Sets the value of fieldOps
     *
     * @param argFieldOps Value to assign to this.fieldOps
     */
    public void setFieldOps(Map argFieldOps) {
        this.fieldOps = argFieldOps;
    }

    /**
     * Set a value in the fieldOps map
     * @param key the key
     * @param value the value
     */
    public void setFieldOp(String key, Object value) {
        fieldOps.put(key, value);
    }

    /**
     * Get a value from the fieldOps map
     * @param key the key
     * @return the value
     */
    public Object getFieldOp(String key) {
        return fieldOps.get(key);
    }

    /**
     * Gets the value of fieldValues
     *
     * @return the value of fieldValues
     */
    public Map getFieldValues()  {
        return this.fieldValues;
    }

    /**
     * Sets the value of fieldValues
     *
     * @param argFieldValues Value to assign to this.fieldValues
     */
    public void setFieldValues(Map argFieldValues) {
        this.fieldValues = argFieldValues;
    }

    /**
     * Set a value in the fieldValues map
     * @param key the key
     * @param value the value
     */
    public void setFieldValue(String key, Object value) {
        fieldValues.put(key, value);
    }

    /**
     * Get a value from the fieldValues map
     * @param key the key
     * @return the value
     */
    public Object getFieldValue(String key) {
        return fieldValues.get(key);
    }

    /**
     * @see Object#equals
     */
    public boolean equals(Object o) {
        if (!(o instanceof DisplayQueryClass)) {
            return false;
        }

        DisplayQueryClass d = (DisplayQueryClass) o;

        return d.getType().equals(getType())
            && d.getConstraintNames().equals(getConstraintNames())
            && d.getFieldNames().equals(getFieldNames())
            && d.getFieldOps().equals(getFieldOps())
            && d.getFieldValues().equals(getFieldValues());
    }

    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return getType().hashCode()
            ^ getConstraintNames().hashCode()
            ^ getFieldNames().hashCode()
            ^ getFieldOps().hashCode()
            ^ getFieldValues().hashCode();
    }

    /**
     * @see Object#toString
     */
    public String toString() {
        return type + " " + constraintNames + " " + fieldNames + " " + fieldOps + " "
            + fieldValues;
    }
}
