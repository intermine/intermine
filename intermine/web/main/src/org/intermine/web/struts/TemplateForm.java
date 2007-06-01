package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.query.Constraint;
import org.intermine.web.logic.query.MainHelper;
import org.intermine.web.logic.query.PathNode;
import org.intermine.web.logic.template.TemplateHelper;
import org.intermine.web.logic.template.TemplateQuery;

/**
 * Form to handle input from the template page
 * @author Mark Woodbridge
 */
public class TemplateForm extends ActionForm
{
    /** Maps containing form state for each constraint. */
    protected Map attributeOps, attributeValues, parsedAttributeValues, useBagConstraint;
    protected Map selectedBags, bagOps;
    protected String templateType, templateName;
    protected String view;
    
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
     * Set value of useBagConstraint for given constraint key.
     * @param key the key
     * @param value the value
     */
    public void setUseBagConstraint(String key, boolean value) {
        useBagConstraint.put(key, value ? Boolean.TRUE : Boolean.FALSE);
    }
    
    /**
     * Get the value of useBagConstraint for given constraint key.
     * @param key the key
     * @return the value
     */
    public boolean getUseBagConstraint(String key) {
        return Boolean.TRUE.equals(useBagConstraint.get(key));
    }
    
    /**
     * Set the bag name.
     * @param key the key
     * @param bag bag name
     */
    public void setBag(String key, Object bag) {
        selectedBags.put(key, bag);
    }
    
    /**
     * Get the bag name selected.
     * @param key the key
     * @return the bag selected
     */
    public Object getBag(String key) {
        return selectedBags.get(key);
    }
    
    /**
     * Get the bag operation selected.
     * @param key the key
     * @return the bag operation selected
     */
    public String getBagOp(String key) {
        return (String) bagOps.get(key);
    }
    
    /**
     * Set bag operation.
     * @param bagOp the bag operation selected
     * @param key the key
     */
    public void setBagOp(String key, String bagOp) {
        bagOps.put(key, bagOp);
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
     * Get the template name.
     * @return the template name
     */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * Set the template name.
     * @param templateName the template name
     */
    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    /**
     * Get the selected alternative view name.
     * @return selected alternative view name
     */
    public String getView() {
        return view;
    }
    
    /**
     * Set the selected alternative view name.
     * @param view selected alternative view name
     */
    public void setView(String view) {
        this.view = view;
    }
    
    /**
     * Get the template type.
     * @return the template type
     */
    public String getTemplateType() {
        return templateType;
    }

    /**
     * Set the template type.
     * @param templateType the template type
     */
    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    /**
     * {@inheritDoc}
     */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        String queryName = getTemplateName();
        String userName = ((Profile) session.getAttribute(Constants.PROFILE)).getUsername();
        
        TemplateQuery template =
            TemplateHelper.findTemplate(servletContext, session, userName, 
                                        queryName, getTemplateType());
        ActionErrors errors = new ActionErrors();
        
        boolean appendWildcard = (request.getParameter("appendWildcard") != null 
                                  && !request.getParameter("appendWildcard").equals("no"));
        parseAttributeValues(template, session, errors, appendWildcard);
               

        return errors;
    }

    /**
     * For each value entered, parse the value into a format that can be
     * applied to the particular constraint.
     * 
     * @param template the related template query
     * @param session the current session
     * @param errors a place to store any parse errors
     * @param appendWildcard if true a "%" will be append to string fields
     */
    public void parseAttributeValues(TemplateQuery template, HttpSession session,
                                     ActionErrors errors, boolean appendWildcard) {
        Locale locale;
        if (session == null) {
            // use default locale
            locale = null;
        } else {
            locale = (Locale) session.getAttribute(Globals.LOCALE_KEY);
        }
        int j = 0;
        for (Iterator i = template.getEditableNodes().iterator(); i.hasNext();) {
            PathNode node = (PathNode) i.next();
            for (Iterator ci = template.getEditableConstraints(node).iterator(); ci.hasNext();) {
                Constraint c = (Constraint) ci.next();
                
                String key = "" + (j + 1);
                Class fieldClass;
                if (node.isAttribute()) {
                    fieldClass = MainHelper.getClass(node.getType());
                } else {
                    fieldClass = String.class;
                }
                
                if (getUseBagConstraint(key)) {
                    // validate choice of bag in some way?
                } else {
                    Integer opIndex = Integer.valueOf((String) getAttributeOps(key));
                    ConstraintOp constraintOp = ConstraintOp.getOpForIndex(opIndex);
                    Object parseVal = QueryBuilderForm.parseValue((String) attributeValues.get(key),
                                                        fieldClass, constraintOp, locale, errors);
                    if (parseVal instanceof String && appendWildcard) {
                         parseVal = ((String) parseVal) + "%";
                    }
                    parsedAttributeValues.put(key, parseVal);
                }
                j++;
            }
        }
    }

    /**
     * {@inheritDoc}
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
        useBagConstraint = new HashMap();
        selectedBags = new HashMap();
        bagOps = new HashMap();
        templateName = null;
        templateType = null;
        view = "";
    }
}
