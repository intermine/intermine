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

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Date;
import java.util.Locale;
import java.util.GregorianCalendar;
import java.text.DateFormat;
import java.text.ParseException;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionError;
import org.apache.struts.Globals;

import org.flymine.objectstore.query.ConstraintOp;
import org.flymine.objectstore.query.BagConstraint;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionMapping;
import org.flymine.util.TypeUtil;
import org.flymine.metadata.Model;
import org.flymine.metadata.FieldDescriptor;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.AttributeDescriptor;
import org.flymine.metadata.presentation.DisplayModel;

/**
 * Form bean to represent the inputs to a text-based query
 *
 * @author Andrew Varley
 */
public class QueryBuildForm extends ActionForm
{
    // field to which a constraint is to be added - used when the user adds a constraint
    protected String newFieldName;
    // map from constraint name to constaintOp index
    protected Map fieldOps = new HashMap();
    // map from constraint name to constraint value (a string)
    protected Map fieldValues = new HashMap();

    /**
     * Set newFieldName
     * @param newFieldName the new field name
     */
    public void setNewFieldName(String newFieldName) {
        this.newFieldName = newFieldName;
    }

    /**
     * Get newFieldName
     * @return the new field name
     */
    public String getNewFieldName() {
        return newFieldName;
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
     * Return a Map of field names to ConstraintOp objects.  The ConstraintOp objects are created
     * by validate().
     *
     * @return a Map of field names to ConstraintOp objects.
     */
    public Map getParsedFieldOps() {
        return parsedFieldOps;
    }

    /**
     * Return a Map of field names to parsed values objects (ie. Integer, Date, etc).  The
     * objects are created by validate().
     *
     * @return a Map of field names to parsed objects.
     */
    public Map getParsedFieldValues() {
        return parsedFieldValues;
    }

    /**
     * Validate the values entered in the form and populate the Maps needed by getParsedFieldOps()
     * and getParsedFieldValues().
     *
     * @param mapping - The mapping used to select this instance
     * @param request - The servlet request we are processing
     * @return ActionErrors object that encapsulates any validation errors
     */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map queryClasses = (Map) session.getAttribute("queryClasses");
        String editingAlias = (String) session.getAttribute("editingAlias");
        
        if (editingAlias == null) {
            // all is well - nothing has happened yet
            return null;
        }
        
        DisplayQueryClass displayQueryClass = (DisplayQueryClass) queryClasses.get(editingAlias);
        ActionErrors errors = new ActionErrors();
        Model model = ((DisplayModel) session.getServletContext()
                       .getAttribute(Constants.MODEL)).getModel();

        ClassDescriptor cd = model.getClassDescriptorByName(displayQueryClass.getType());
        Class selectClass = cd.getType();

        Locale locale = (Locale) session.getAttribute(Globals.LOCALE_KEY);

        for (Iterator i = displayQueryClass.getConstraintNames().iterator(); i.hasNext();) {
            String fieldName = (String) i.next();

            Object fieldValue = getFieldValue(fieldName);
            Object fieldOp = getFieldOp(fieldName);

            if (fieldValue == null || fieldOp == null) {
                continue;
            }
            
            Integer opCode = Integer.valueOf((String) fieldOp);
            ConstraintOp op = ConstraintOp.getOpForIndex(opCode);
            parsedFieldOps.put(fieldName, op);
            String realFieldName = QueryBuildHelper.getFieldName(fieldName);

            Map fieldDescriptors = model.getFieldDescriptorsForClass(selectClass);
            FieldDescriptor fd = (FieldDescriptor) fieldDescriptors.get(realFieldName);

            if (fd.isAttribute()) {
                ActionError actionError =
                    validateAttribute((AttributeDescriptor) fd, op, fieldName, fieldValue,
                                      locale);

                if (actionError != null) {
                    errors.add(fieldName, actionError);
                }
            }
        }

        return errors;
    }

    /**
     * Check that one field is valid and update parsedFieldOps and parsedFieldValues.
     *
     * @param attributeDescriptor the descriptor used to get type information for parsing the
     *        fieldValue
     * @param op the ConstraintOp on this field
     * @param fieldName the name of the field from the form
     * @param fieldName the value from the form
     * @param locale the current session Locale
     * @return an ActionError describing a parse problem, or null if there are no problems
     */
    private ActionError validateAttribute(AttributeDescriptor attributeDescriptor,
                                          ConstraintOp op, String fieldName, Object fieldValue,
                                          Locale locale) {
        Class fieldClass = TypeUtil.instantiate(attributeDescriptor.getType());

        parsedFieldOps.put(fieldName, op);

        try {
            if (BagConstraint.VALID_OPS.contains(op)) {
                // the value has to be the name of a saved bag or a saved query.  leave it
                // as a String for now
                parsedFieldValues.put(fieldName, fieldValue);
            } else {
                String stringFieldValue = (String) fieldValue;
                        
                if (stringFieldValue != null) {
                    if (fieldClass.equals(Date.class)) {
                        DateFormat dateFormat =
                            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT,
                                                           locale);
                        parsedFieldValues.put(fieldName, dateFormat.parse(stringFieldValue));
                    } else {
                        parsedFieldValues.put(fieldName, TypeUtil.stringToObject(fieldClass,
                                                                                 stringFieldValue));
                    }
                }
            }
        } catch (ParseException e) {
            Date dateExample = new GregorianCalendar().getTime();
            return new ActionError("errors.date", fieldValue, dateExample);
        } catch (NumberFormatException e) {
            String shortClassName =
                fieldClass.getName().substring(fieldClass.getName().lastIndexOf(".") + 1);
            return new ActionError("errors." + shortClassName.toLowerCase(), fieldValue);
        }

        return null;
    }

    private Map parsedFieldValues = new HashMap();
    private Map parsedFieldOps = new HashMap();
}
