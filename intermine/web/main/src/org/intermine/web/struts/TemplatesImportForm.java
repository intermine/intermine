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

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.validator.ValidatorForm;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.template.TemplateHelper;

/**
 * Form bean representing template import form.
 *
 * @author  Thomas Riley
 */
public class TemplatesImportForm extends ValidatorForm
{
    private String xml;
    private boolean overwriting = false;
    
    /**
     * Creates a new instance of TemplatesImportForm.
     */
    public TemplatesImportForm() {
        reset();
    }
    
    /**
     * Get the xml.
     * @return templates in xml format
     */
    public String getXml() {
        return xml;
    }

    /**
     * Set the xml.
     * @param xml templates in xml format
     */
    public void setXml(String xml) {
        this.xml = xml;
    }
    
    /**
     * Get the overwrite flag.
     * @return  true to overwrite existing template, false to add
     */
    public boolean isOverwriting() {
        return overwriting;
    }
    
    /**
     * Set the overwriting flag.
     * @param overwriting true to overwrite existing templates, false to add
     */
    public void setOverwriting(boolean overwriting) {
        this.overwriting = overwriting;
    }
    
    /**
     * {@inheritDoc}
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        reset();
    }
    
    /**
     * Reset the form.
     */
    protected void reset() {
        xml = "";
        overwriting = false;
    }

    /**
     * Call inherited method then check whether xml is valid.
     *
     * {@inheritDoc}
     */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ServletContext servletContext = session.getServletContext();
        Map classKeys = (Map) servletContext.getAttribute(Constants.CLASS_KEYS);
        ActionErrors errors = super.validate(mapping, request);
        if (errors != null && errors.size() > 0) {
            return errors;
        }
        try {
           TemplateHelper.xmlToTemplateMap(getXml(), profile.getSavedBags(), servletContext);
        } catch (Exception err) {
            if (errors == null) {
                errors = new ActionErrors();
            }
            errors.add(ActionErrors.GLOBAL_MESSAGE,
                        new ActionMessage("errors.badtemplatexml", err.getMessage()));
        }
        return errors;
    }
}
