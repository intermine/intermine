package org.flymine.web;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.HashMap;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

/**
 * Form bean to represent the inputs to a text-based query
 *
 * @author Andrew Varley
 */
public class QueryForm extends ActionForm
{
    protected Map fieldValues = new HashMap();
    protected Map fieldOps = new HashMap();

    /**
     * Set the map field name/values for QueryClass
     *
     * @param fieldValues a map of fieldname/value
     */
    public void setFieldValues(Map fieldValues) {
        this.fieldValues = fieldValues;
    }

    /**
     * Get the map of field values
     *
     * @return the map of field values
     */
    public Map getFieldValues() {
        return this.fieldValues;
    }

    /**
     * Set the map field name/values for QueryClass
     *
     * @param fieldOps a map of fieldname/operation
     */
    public void setFieldOps(Map fieldOps) {
        this.fieldOps = fieldOps;
    }

    /**
     * Get the map of field values
     *
     * @return the map of field values
     */
    public Map getFieldOps() {
        return this.fieldOps;
    }

    /**
     * Set a value for the given field of QueryClass
     *
     * @param key the field name
     * @param value value to set
     */
    public void setFieldValue(String key, Object value) {
        fieldValues.put(key, value);
    }

    /**
     * Get the value for the given field
     *
     * @param key the field name
     * @return the field value
     */
    public Object getFieldValue(String key) {
        return fieldValues.get(key);
    }
    /**
     * Set a value for the given field of QueryClass
     *
     * @param key the field name
     * @param value value to set
     */
    public void setFieldOp(String key, Object value) {
        fieldOps.put(key, value);
    }

    /**
     * Get the value for the given field
     *
     * @param key the field name
     * @return the field value
     */
    public Object getFieldOp(String key) {
        return fieldOps.get(key);
    }
    /**
     * @see ActionForm#reset
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        fieldValues.clear();
        fieldOps.clear();
    }
}
