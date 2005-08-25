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

import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.util.Map;
import java.util.HashMap;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.ValidatorForm;

/**
 * Form bean for template building page.
 *
 * @author Thomas Riley
 */
public class BuildTemplateForm extends ValidatorForm
{
    /** Map from constraint key to boolean indicating constraint editability. */
    protected Map editableConstraints;
    /** Map from constraint to constraint label. */
    protected Map constraintLabels;
    /** Map from constraint to constraint identifier. */
    protected Map constraintIds;
    /** Template description. */
    protected String templateDescription = "";
    /** Template name. */
    protected String shortName = "";
    /** Is the template 'important'? (Shown early on portal entry page) */
    protected boolean important = false;
    /** Search keywords - comma seperated. */
    protected String keywords = "";
    
    /**
     * Construct instance of BuildTemplateForm.
     */
    public BuildTemplateForm() {
        super();
        reset();
    }
    
    /**
     * Set the template description
     *
     * @param templateDescription the template description
     */
    public void setDescription(String templateDescription) {
        this.templateDescription = templateDescription;
    }

    /**
     * Get the template description
     *
     * @return the template description
     */
    public String getDescription() {
        return templateDescription;
    }
    
    /**
     * Set the template short name
     *
     * @param name the template short name
     */
    public void setShortName(String name) {
        shortName = name;
    }
    
    /**
     * Get the template short name
     *
     * @return  the template short name
     */
    public String getShortName() {
        return shortName;
    }
    
    /**
     * Get the editable state of a constraint.
     *
     * @param index  the constraint index
     * @return       true if constraint is editable, false if not
     */
    public boolean getConstraintEditable(String index) {
        if (editableConstraints.get(index) == null) {
            return false;
        }   
        return (editableConstraints.get(index) == Boolean.TRUE);
    }
    
    /**
     * Set editability of constraint.
     *
     * @param index  constraint index
     * @param value  boolean editability value
     */
    public void setConstraintEditable(String index, boolean value) {
        editableConstraints.put(index, value ? Boolean.TRUE : Boolean.FALSE);
    }
    
    /**
     * Get a constraint identifier.
     *
     * @param index  constraint index key
     * @return       identifier for constraint
     */
    public Object getConstraintIdentifier(String index) {
        return constraintIds.get(index);
    }
    
    /**
     * Set constraint identifier.
     *
     * @param index  constraint index key
     * @param value  constraint identifier
     */
    public void setConstraintIdentifier(String index, Object value) {
        constraintIds.put(index, value);
    }
    
    /**
     * Get a constraint label.
     *
     * @param index  constraint index key
     * @return       label for constraint
     */
    public Object getConstraintLabel(String index) {
        return constraintLabels.get(index);
    }
    
    /**
     * Set constraint label.
     *
     * @param index  constraint index key
     * @param value  constraint label
     */
    public void setConstraintLabel(String index, Object value) {
        constraintLabels.put(index, value);
    }
    
    /**
     * Whether or not this template be marked as important.
     *
     * @return whether or not this template be marked as important.
     */
    public boolean isImportant() {
        return important;
    }

    /**
     * @param important whether or not this template should be marked as important.
     */
    public void setImportant(boolean important) {
        this.important = important;
    }
    
    /**
     * Get the keywords.
     * 
     * @return the keywords of this template
     */
    public String getKeywords() {
        return keywords;
    }
    
    /**
     * Set the keywords.
     * 
     * @param keywords keywords for this template
     */
    
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }
    
    /**
     * Fail validation if user template exists with specified name or no
     * constraints have been made editable.
     *
     * @see ActionForm#validate
     */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Profile profile = (Profile) request.getSession().getAttribute(Constants.PROFILE);
        Map userTemplates = profile.getSavedTemplates();
        
        ActionErrors errors = super.validate(mapping, request);
        if (shortName != null && session.getAttribute(Constants.EDITING_TEMPLATE) == null
            && userTemplates.containsKey(shortName)) {
            if (errors == null) {
                errors = new ActionErrors();
            }
            errors.add(ActionMessages.GLOBAL_MESSAGE,
                       new ActionMessage("errors.createtemplate.existing", shortName));
        }
        if (editableConstraints.size() == 0) {
            if (errors == null) {
                errors = new ActionErrors();
            }
            errors.add(ActionMessages.GLOBAL_MESSAGE,
                       new ActionMessage("errors.createtemplate.nothingeditable"));
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
     * Clear all properties.
     */
    protected void reset() {
        editableConstraints = new HashMap();
        constraintLabels = new HashMap();
        constraintIds = new HashMap();
        templateDescription = "";
        shortName = "";
        keywords = "";
        important = false;
    }
    
    /**
     * Initialise this form bean with values taken from an existing
     * template query.
     *
     * @param template  an existing template query
     */
    public void initFromTemplate(TemplateQuery template) {
        setShortName(template.getName());
        setDescription(template.getDescription());
        setImportant(template.isImportant());
        setKeywords(template.getKeywords());
        
        int j = 0;
        Iterator niter = template.getQuery().getNodes().entrySet().iterator();
        
        while (niter.hasNext()) {
            Map.Entry entry = (Map.Entry) niter.next();
            PathNode node = (PathNode) entry.getValue();
            
            if (node.isAttribute()) {
                Iterator citer = node.getConstraints().iterator();
                while (citer.hasNext()) {
                    Constraint c = (Constraint) citer.next();
                    String key = "" + (j + 1);
                    setConstraintLabel(key, c.getDescription());
                    setConstraintEditable(key, c.isEditable());
                    setConstraintIdentifier(key, c.getIdentifier());
                    j++;
                }
            }
        }
    }
}
