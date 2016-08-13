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

/**
 * Form bean used on the saved template form.
 * @author Thomas Riley
 */
public class ModifyTemplateForm extends ActionForm
{
    protected String[] selected;
    protected String pageName; // can modify template from templates and mymine pages
    protected String templateButton;

    /**
     * Constructor
     */
    public ModifyTemplateForm() {
        initialise();
    }

    /**
     * Initialiser
     */
    public void initialise() {
        selected = new String[0];
    }

    /**
     * Sets the selected templates
     *
     * @param selected the selected templates
     */
    public void setSelected(String[] selected) {
        this.selected = selected;
    }

    /**
     * Gets the selected templates
     *
     * @return the selected templates
     */
    public String[] getSelected() {
        return selected;
    }

    /**
     * @return the pageName
     */
    public String getPageName() {
        return pageName;
    }

    /**
     * @param pageName the pageName to set
     */
    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    /**
     * @param templateButton the template button pressed
     */
    public void setTemplateButton(String templateButton) {
        this.templateButton = templateButton;
    }

    /**
     * @return the template button
     */
    public String getTemplateButton() {
        return templateButton;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ActionErrors validate(ActionMapping mapping,
                                 HttpServletRequest request) {
        ActionErrors errors = null;
        if (selected.length == 0) {
            errors = new ActionErrors();
            errors.add(ActionMessages.GLOBAL_MESSAGE,
                    new ActionMessage("errors.modifyTemplate.noselect"));
        }
        return errors;
    }

    /**
     * Reset the form to the initial state
     *
     * @param mapping the mapping
     * @param request the request
     */
    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        initialise();
    }
}
