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
        Locale locale = (Locale) session.getAttribute(Globals.LOCALE_KEY);
        Map templateQueries = (Map) servletContext.getAttribute(Constants.TEMPLATE_QUERIES);
        String queryName = (String) session.getAttribute("queryName");
        
        TemplateQuery template = (TemplateQuery) templateQueries.get(queryName);
        
        ActionErrors errors = new ActionErrors();
        
        for (Iterator i = attributeValues.keySet().iterator(); i.hasNext();) {
            String j = (String) i.next();
            PathNode node = (PathNode) template.getNodes().get(Integer.parseInt(j) - 1);
            Class fieldClass = MainHelper.getClass(node.getType());
            ConstraintOp constraintOp = ConstraintOp.getOpForIndex(Integer.valueOf((String) getAttributeOps(j)));
            parsedAttributeValues.put(j, MainForm.parseValue((String) attributeValues.get(j),
                                                             fieldClass, constraintOp, locale, errors));
        }

        return errors;
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
