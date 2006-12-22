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
import javax.servlet.http.HttpSession;

import java.util.Map;

import org.apache.struts.upload.FormFile;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

/**
 * Form bean to represent the inputs needed to create a bag from user input.
 *
 * @author Kim Rutherford
 */

public class BuildBagForm extends ActionForm
{
    protected String bagName;
    protected FormFile formFile;
    protected String text;
    protected String type;

    /**
     * Get the bag type
     * @return the bag type string
     */
    public String getType() {
        return type;
    }

    /**
     * Set the bag type
     * @param type the bag type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Set the query string
     *
     * @param text the query string
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Get the text string
     *
     * @return the text string
     */
    public String getText() {
        return text;
    }

    /**
     * Set the FormFile.
     * @param formFile the FormFile
     */
    public void setFormFile(FormFile formFile) {
        this.formFile = formFile;
    }

    /**
     * Get the FormFile.
     * @return the FormFile.
     */
    public FormFile getFormFile() {
        return formFile;
    }

    /**
     * Set the bag name (existing bags)
     *
     * @param bagName the bag name to save to
     */
    public void setBagName(String bagName) {
        this.bagName = bagName;
    }

    /**
     * Get the bag name (existing bags)
     *
     * @return the bag name
     */
    public String getBagName() {
        return bagName;
    }

    /**
     * @see ActionForm#validate
     */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        
        Map savedBags = profile.getSavedBags();

        ActionErrors errors = null;

        if (request.getParameter("action") != null) {
            if (bagName.equals("")) {
                errors = new ActionErrors();
                errors.add(ActionMessages.GLOBAL_MESSAGE,
                           new ActionMessage("errors.savebag.blank"));
            } else if (savedBags != null && savedBags.containsKey(bagName)) {
                errors = new ActionErrors();
                errors.add(ActionMessages.GLOBAL_MESSAGE,
                           new ActionMessage("errors.savebag.existing", bagName));
            }
        }

        return errors;
    }
}
