package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
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

import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Locale;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionErrors;

import org.intermine.objectstore.query.ConstraintOp;

/**
 * Form to handle input from the template page
 * @author Mark Woodbridge
 */
public class TemplateForm extends ActionForm
{
    protected Map attributeOps, attributeValues, parsedAttributeValues;

    /**
     * Constructor
     */
    public TemplateForm() {
        super();
        reset();
    }

    /**
     * Set the attribute ops
     * @param attributeOps the attribute ops
     */
    public void setAttributeOps(Map attributeOps) {
        this.attributeOps = attributeOps;
    }

    /**
     * Get the attribute ops
     * @return the attribute ops
     */
    public Map getAttributeOps() {
        return attributeOps;
    }

    /**
     * Set an attribute op
     * @param key the key
     * @param value the value
     */
    public void setAttributeOps(String key, Object value) {
        attributeOps.put(key, value);
    }
    
    /**
     * Get an attribute op
     * @param key the key
     * @return the value
     */
    public Object getAttributeOps(String key)  {
        return attributeOps.get(key);
    }

    /**
     * Set the attribute values
     * @param attributeValues the attribute values
     */
    public void setAttributeValues(Map attributeValues) {
        this.attributeValues = attributeValues;
    }

    /**
     * Get the attribute values
     * @return the attribute values
     */
    public Map getAttributeValues() {
        return attributeValues;
    }

    /**
     * Set an attribute value
     * @param key the key
     * @param value the value
     */
    public void setAttributeValues(String key, Object value) {
        attributeValues.put(key, value);
    }

    /**
     * Get an attribute value
     * @param key the key
     * @return the value
     */
    public Object getAttributeValues(String key)  {
        return attributeValues.get(key);
    }

    /**
     * Get a parsed attribute value
     * @param key the key
     * @return the value
     */
    public Object getParsedAttributeValues(String key) {
        return parsedAttributeValues.get(key);
    }

    /**
     * @see ActionForm#validate
     */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        String queryName = request.getParameter("queryName");
        String templateType = request.getParameter("templateType");
        
        if (templateType == null) {
            templateType = (String) session.getAttribute("templateType");
        }
        
        if (queryName == null) {
            queryName = (String) session.getAttribute("queryName");
        }
        
        TemplateQuery template = TemplateHelper.findTemplate(request, queryName, templateType);
        
        ActionErrors errors = new ActionErrors();
        
        parseAttributeValues(template, session, errors);

        return errors;
    }
    
    /**
     * For each value entered, parse the value into a format that can be
     * applied to the particular constraint.
     * 
     * @param template the related template query
     * @param session the current session
     * @param errors a place to store any parse errors
     */
    protected void parseAttributeValues(TemplateQuery template, HttpSession session,
                                        ActionErrors errors) {
        Locale locale = (Locale) session.getAttribute(Globals.LOCALE_KEY);
        int j = 0;
        for (Iterator i = template.getNodes().iterator(); i.hasNext();) {
            PathNode node = (PathNode) i.next();
            for (Iterator ci = template.getConstraints(node).iterator(); ci.hasNext();) {
                Constraint c = (Constraint) ci.next();
                
                String key = "" + (j + 1);
                Class fieldClass = MainHelper.getClass(node.getType());
                Integer opIndex = Integer.valueOf((String) getAttributeOps(key));
                ConstraintOp constraintOp = ConstraintOp.getOpForIndex(opIndex);
                Object parseVal = MainForm.parseValue((String) attributeValues.get(key),
                                                    fieldClass, constraintOp, locale, errors);
                parsedAttributeValues.put(key, parseVal);
                j++;
            }
        }
    }

    /**
     * @see ActionForm#reset
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        reset();
    }
    
    /**
     * Reset the form
     */
    protected void reset() {
        attributeOps = new HashMap();
        attributeValues = new HashMap();
        parsedAttributeValues = new HashMap();
    }
}
