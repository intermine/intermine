package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.util.Date;
import java.util.Locale;
import java.text.DateFormat;
import java.text.ParseException;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionError;

import org.intermine.util.TypeUtil;

/**
 * The main form, using for editing constraints
 * @author Mark Woodbridge
 */
public class MainForm extends ActionForm
{
    protected String bagOp, bagValue;
    protected String attributeOp, attributeValue;
    protected String subclassValue;

    protected String path;

    protected Object parsedAttributeValue;

    /**
     * Gets the value of bagOp
     *
     * @return the value of bagOp
     */
    public String getBagOp()  {
        return bagOp;
    }

    /**
     * Sets the value of bagOp
     *
     * @param bagOp Value to assign to bagOp
     */
    public void setBagOp(String bagOp) {
        this.bagOp = bagOp;
    }

    /**
     * Gets the value of bagValue
     *
     * @return the value of bagValue
     */
    public String getBagValue()  {
        return bagValue;
    }

    /**
     * Sets the value of bagValue
     *
     * @param bagValue value to assign to bagValue
     */
    public void setBagValue(String bagValue) {
        this.bagValue = bagValue;
    }

    /**
     * Gets the value of attributeOp
     *
     * @return the value of attributeOp
     */
    public String getAttributeOp()  {
        return attributeOp;
    }

    /**
     * Sets the value of attributeOp
     *
     * @param attributeOp value to assign to attributeOp
     */
    public void setAttributeOp(String attributeOp) {
        this.attributeOp = attributeOp;
    }

    /**
     * Gets the value of attributeValue
     *
     * @return the value of attributeValue
     */
    public String getAttributeValue()  {
        return attributeValue;
    }

    /**
     * Sets the value of attributeValue
     *
     * @param attributeValue value to assign to attributeValue
     */
    public void setAttributeValue(String attributeValue) {
        this.attributeValue = attributeValue;
    }

    /**
     * Gets the value of subclassValue
     *
     * @return the value of subclassValue
     */
    public String getSubclassValue()  {
        return subclassValue;
    }

    /**
     * Sets the value of subclassValue
     *
     * @param subclassValue value to assign to subclassValue
     */
    public void setSubclassValue(String subclassValue) {
        this.subclassValue = subclassValue;
    }

    /**
     * Gets the value of path
     *
     * @return the value of path
     */
    public String getPath()  {
        return path;
    }

    /**
     * Sets the value of path
     *
     * @param path value to assign to path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Gets the value of parsedAttributeValue
     *
     * @return the value of parsedAttributeValue
     */
    public Object getParsedAttributeValue()  {
        return parsedAttributeValue;
    }

    /**
     * Sets the value of parsedAttributeValue
     *
     * @param parsedAttributeValue value to assign to parsedAttributeValue
     */
    public void setParsedAttributeValue(Object parsedAttributeValue) {
        this.parsedAttributeValue = parsedAttributeValue;
    }

    /**
     * @see ActionForm#validate
     */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Locale locale = (Locale) session.getAttribute(Globals.LOCALE_KEY);
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);

        ActionErrors errors = new ActionErrors();

        if (request.getParameter("attribute") != null) {
            PathNode node = (PathNode) query.getNodes().get(path);
            Class fieldClass = MainHelper.getClass(node.getType());
            parsedAttributeValue = parseValue(attributeValue, fieldClass, locale, errors);
        }

        if (errors.size() > 0) {
            request.setAttribute("editingNode", query.getNodes().get(path));
        }
        
        return errors;
    }
    
    /**
     * Parse an attribute value
     * @param value the value as a String
     * @param type the type of the parsed value
     * @param locale the user's locale
     * @param errors ActionErrors to which any parse errors are added
     * @return the parsed value
     */
    public static Object parseValue(String value, Class type, Locale locale, ActionErrors errors) {
        Object parsedValue = null;
        if (Date.class.equals(type)) {
            DateFormat df =  DateFormat.getDateInstance(DateFormat.SHORT, locale);
            try {
                parsedValue = df.parse(value);
            } catch (ParseException e) {
                errors.add(ActionErrors.GLOBAL_ERROR,
                           new ActionError("errors.date", value, df.format(new Date())));
            }
        } else {
            try {
                parsedValue = TypeUtil.stringToObject(type, value);
            } catch (NumberFormatException e) {
                String shortName = TypeUtil.unqualifiedName(type.getName()).toLowerCase();
                errors.add(ActionErrors.GLOBAL_ERROR,
                           new ActionError("errors." + shortName, value));
            }
        }
        return parsedValue;
    }

    /**
     * @see ActionForm#reset
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        bagOp = null;
        bagValue = null;
        attributeOp = null;
        attributeValue = null;
        subclassValue = null;
        path = null;
    }
}
