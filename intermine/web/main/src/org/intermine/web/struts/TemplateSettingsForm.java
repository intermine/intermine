package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.intermine.api.util.NameUtil;
import org.intermine.template.TemplateQuery;
import org.intermine.web.logic.session.SessionMethods;

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
    private String actionType = "";

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
     * {@inheritDoc}
     */
    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        TemplateQuery template = (TemplateQuery) SessionMethods.getQuery(request.getSession());
        setName(template.getName());
        setTitle(template.getTitle());
        setDescription(template.getDescription());
        setComment(template.getComment());
    }

    @Override
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errors = new ActionErrors();
        if (!NameUtil.isValidName(name)) {
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.badChars"));
        }
        return errors;
    }

    /** @return the type of action **/
    public String getActionType() {
        return actionType;
    }

    /** @param actionType the type of action **/
    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

}
