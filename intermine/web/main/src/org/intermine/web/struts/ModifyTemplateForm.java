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
     * @see ActionForm#validate
     */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
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
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        initialise();
    }
}
