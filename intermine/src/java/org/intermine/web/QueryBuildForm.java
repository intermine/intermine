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
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Date;
import java.util.Locale;
import java.util.GregorianCalendar;
import java.text.DateFormat;
import java.text.ParseException;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletContext;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.Globals;

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.util.TypeUtil;
import org.intermine.metadata.Model;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.AttributeDescriptor;

/**
 * Form bean to represent the inputs to a text-based query
 *
 * @author Andrew Varley
 */
public class QueryBuildForm extends ActionForm
{
    // new alias of class (eg. Department_0)
    protected String newClassName = null;
    // field to which a constraint is to be added - used when the user adds a constraint
    protected String newFieldName = null;
    // map from constraint name to constaintOp index
    protected Map fieldOps = new HashMap();
    // map from constraint name to constraint value (a string)
    protected Map fieldValues = new HashMap();
    // map from "name" of last button pressed to text of that button
    protected Map buttons = new HashMap();

    /**
     * Set the new class name
     * @param newClassName the new class name
     */
    public void setNewClassName(String newClassName) {
        this.newClassName = newClassName;
    }

    /**
     * Get newClassName
     * @return the new class name
     */
    public String getNewClassName() {
        return newClassName;
    }

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
     * Set the buttons map
     *
     * @param buttons the map
     */
    public void setButtons(Map buttons) {
        this.buttons = buttons;
    }

    /**
     * Get the buttons map
     *
     * @return the map
     */
    public Map getButtons() {
        return this.buttons;
    }

    /**
     * Set a value for the given field
     *
     * @param key the field name
     * @param value value to set
     */
    public void setButton(String key, Object value) {
        buttons.put(key, value);
    }

    /**
     * Get the value for the given field
     *
     * @param key the field name
     * @return the field value
     */
    public Object getButton(String key) {
        return buttons.get(key);
    }

    /**
     * Return the name of the last button pressed
     * @return the button name
     */
    public String getButton() {
        if (buttons.size() == 0) {
            return "";
        } else {
            return (String) buttons.keySet().iterator().next();
        }
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
     * @see ActionForm#reset
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        fieldValues = new HashMap();
        fieldOps = new HashMap();
        parsedFieldValues = new HashMap();
        parsedFieldOps = new HashMap();
        newFieldName = null;
        buttons = new HashMap();
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
        //only validate if the button clicked was "update"
        if (!buttons.containsKey("updateClass")) {
            return null;
        }

        //get relevant stuff off session
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Map queryClasses = (Map) session.getAttribute(Constants.QUERY_CLASSES);
        Map savedBags = (Map) session.getAttribute(Constants.SAVED_BAGS);
        String editingAlias = (String) session.getAttribute(Constants.EDITING_ALIAS);
        Model model = (Model) servletContext.getAttribute(Constants.MODEL);
        Locale locale = (Locale) session.getAttribute(Globals.LOCALE_KEY);
        
        DisplayQueryClass d = (DisplayQueryClass) queryClasses.get(editingAlias);

        //initialise values to be returned/updated
        ActionErrors errors = new ActionErrors();
        parsedFieldValues = new HashMap();
        parsedFieldOps = new HashMap();

        //first validate constraints
        for (Iterator i = new ArrayList(d.getConstraintNames()).iterator(); i.hasNext();) {
            String constraintName = (String) i.next();
            String fieldName = (String) d.getFieldNames().get(constraintName);
            String fieldValue = (String) getFieldValue(constraintName);
            String fieldOp = (String) getFieldOp(constraintName);

            ConstraintOp op = ConstraintOp.getOpForIndex(Integer.valueOf(fieldOp));

            Map fieldDescriptors = model.
                getFieldDescriptorsForClass(TypeUtil.instantiate(d.getType()));
            FieldDescriptor fd = (FieldDescriptor) fieldDescriptors.get(fieldName);

            if (fd.isAttribute()) {
                ActionError error = validateAttribute((AttributeDescriptor) fd, op, constraintName,
                                                      fieldValue, locale, savedBags);
                if (error == null) {
                    parsedFieldOps.put(constraintName, op);
                } else {
                    errors.add(constraintName, error);
                }
            } else if (fd.isReference() || fd.isCollection()) {
                //it's possible we presented the user with a blank drop-down
                if (fieldValue == null || fieldValue.equals("")) {
                    d.getConstraintNames().remove(constraintName);
                    d.getFieldNames().remove(constraintName);
                } else {
                    //don't need to check this - we constructed the drop-down
                    parsedFieldOps.put(constraintName, op);
                    parsedFieldValues.put(constraintName, fieldValue);
                }
            }
        }

        //then class alias
        if (newClassName != null) {
            if (newClassName.equals("")) {
                errors.add(newClassName, new ActionError("errors.invalidname"));
            }
            if (!newClassName.equals(editingAlias) && queryClasses.containsKey(newClassName)) {
                errors.add(newClassName, new ActionError("errors.duplicatename", newClassName));
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
     * @param fieldValue the value from the form
     * @param locale the current session Locale
     * @param savedBags the saved bag Map from the session
     * @return an ActionError describing a parse problem, or null if there are no problems
     */
    protected ActionError validateAttribute(AttributeDescriptor attributeDescriptor,
                                            ConstraintOp op, String fieldName, Object fieldValue,
                                            Locale locale, Map savedBags) {
        Class fieldClass = TypeUtil.instantiate(attributeDescriptor.getType());

        try {
            if (BagConstraint.VALID_OPS.contains(op)) {
                if (savedBags.containsKey(fieldValue)) {
                    // the value has to be the name of a saved bag or a saved query.  leave it
                    // as a String for now
                    parsedFieldValues.put(fieldName, fieldValue);
                } else {
                    return new ActionError("errors.bagconstraint", fieldValue);
                }
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
