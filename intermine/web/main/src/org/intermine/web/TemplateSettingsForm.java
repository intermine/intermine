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

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

/**
 * 
 * 
 * @author Thomas Riley
 */
public class TemplateSettingsForm extends ActionForm
{
    private String description = "";
    private String name = "";
    private String keywords = "";
    private boolean important;
    
    /**
     * @return Returns the description.
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * @param description The description to set.
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * @return Returns the important.
     */
    public boolean isImportant() {
        return important;
    }
    
    /**
     * @param important The important to set.
     */
    public void setImportant(boolean important) {
        this.important = important;
    }
    
    /**
     * @return Returns the keywords.
     */
    
    public String getKeywords() {
        return keywords;
    }
    
    /**
     * @param keywords The keywords to set.
     */
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }
    
    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }
    
    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Reset the form bean taking initial state from current TemplateBuildState session
     * attribute.
     * @see ActionForm#reset
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        TemplateBuildState tbs =
            (TemplateBuildState) request.getSession().getAttribute(Constants.TEMPLATE_BUILD_STATE);
        setName(tbs.getName());
        setKeywords(tbs.getKeywords());
        setImportant(tbs.isImportant());
        setDescription(tbs.getDescription());
    }
}
