package org.intermine.web.results;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;

import org.intermine.web.Constants;
import org.intermine.web.Profile;
import org.intermine.web.WebUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

/**
 * Form bean for the results table and bag creation form.
 *
 * @author Andrew Varley
 * @author Thomas Riley
 */
public class SaveBagForm extends ActionForm
{

    protected String existingBagName, newBagName;
    protected String[] selectedObjects;

    /**
     * Constructor
     */
    public SaveBagForm() {
        initialise();
    }

    /**
     * Initialiser
     */
    public void initialise() {
        newBagName = null;
        selectedObjects = new String[0];
    }

    /**
     * Sets the selected objects
     *
     * @param selectedObjects the selected objects
     */
    public void setSelectedObjects(String[] selectedObjects) {
        this.selectedObjects = selectedObjects;
    }

    /**
     * Gets the selected objects
     *
     * @return the selected objects
     */
    public String[] getSelectedObjects() {
        return selectedObjects;
    }
 
    /**
     * Gets the value of existingBagName
     *
     * @return the value of existingBagName
     */
    public String getExistingBagName()  {
        return existingBagName;
    }

    /**
     * Sets the value of existingBagName
     *
     * @param existingBagName Value to assign to this.existingBagName
     */
    public void setExistingBagName(String existingBagName) {
        this.existingBagName = existingBagName;
    }

    /**
     * Gets the value of newBagName
     *
     * @return the value of newBagName
     */
    public String getNewBagName()  {
        return newBagName;
    }

    /**
     * Sets the value of newBagName
     *
     * @param newBagName Value to assign to this.newBagName
     */
    public void setNewBagName(String newBagName) {
        this.newBagName = newBagName;
    }

    /**
     * @see ActionForm#validate
     */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        
        Map savedBags = profile.getSavedBags();
        ActionErrors errors = null;
        
        if ((request.getParameter("addToExistingBag") != null
             || request.getParameter("saveNewBag") != null)
            && selectedObjects.length == 0) {
            errors = new ActionErrors();
            errors.add(ActionMessages.GLOBAL_MESSAGE,
                       new ActionMessage("errors.savebag.nothingSelected"));
        }
        
        if (request.getParameter("saveNewBag") != null) {
            if (newBagName.equals("")) {
                errors = new ActionErrors();
                errors.add(ActionMessages.GLOBAL_MESSAGE,
                           new ActionMessage("errors.savebag.blank"));
            } else if (!WebUtil.isValidName(newBagName)) { 
                errors = new ActionErrors();
                errors.add(ActionMessages.GLOBAL_MESSAGE,
                           new ActionMessage("errors.badChars"));
            } else if (savedBags != null && savedBags.containsKey(newBagName)) {
                errors = new ActionErrors();
                errors.add(ActionMessages.GLOBAL_MESSAGE,
                           new ActionMessage("errors.savebag.existing", newBagName));
            }
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
