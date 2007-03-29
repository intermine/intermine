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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.template.TemplateBuildState;

/**
 * Form used when building a template.
 * 
 * @author Thomas Riley
 */
public class TemplateSettingsForm extends ActionForm
{
    private String description = "";
    private String name = "";
    private String title = "";
    private String comment = "";
    private String keywords = "";
    private boolean important;
    
    /**
     * Return the description.
     * @return the description.
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Set the description
     * @param description The description to set.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Return the title
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set the title
     * @param title the title
     */
    public void setTitle(String title) {
        this.title = title;
    }
    
    /**
     * Return the comment
     * @return the comment
     */
    public String getComment() {
        return comment;
    }
    
    /**
     * Set the comment
     * @param comment the comment
     */
    public void setComment(String comment) {
        this.comment = comment;
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
        setTitle(tbs.getTitle());
        setKeywords(tbs.getKeywords());
        setImportant(tbs.isImportant());
        setDescription(tbs.getDescription());
        setComment(tbs.getComment());
    }
    
    /**
     * @see ActionForm#validate
     */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
       
        ActionErrors errors = null;

        if (!WebUtil.isValidName(name)) { 
            errors = new ActionErrors();
            errors.add(ActionMessages.GLOBAL_MESSAGE,
                       new ActionMessage("errors.badChars"));

        }
        return errors;
    }
}
