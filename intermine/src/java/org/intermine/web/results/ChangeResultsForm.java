package org.intermine.web.results;

/*
 * Copyright (C) 2002-2004 FlyMine
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
import java.util.HashMap;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import org.intermine.web.Constants;

/**
 * Form bean to represent the inputs to a text-based query
 *
 * @author Andrew Varley
 */
public class ChangeResultsForm extends ActionForm
{

    protected String pageSize = "10";
    protected String[] selectedObjects = {};
    protected String bagName;
    protected String newBagName;
    // map from "name" of last button pressed to text of that button
    protected Map buttons = new HashMap();

    /**
     * Set the page size
     *
     * @param pageSize the page size to display
     */
    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Get the page size
     *
     * @return the page size
     */
    public String getPageSize() {
        return pageSize;
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
     * Set the bag name (new bag)
     *
     * @param bagName the bag name to save to
     */
    public void setNewBagName(String bagName) {
        this.newBagName = bagName;
    }

    /**
     * Get the bag name (new bag)
     *
     * @return the bag name
     */
    public String getNewBagName() {
        return newBagName;
    }

    /**
     * Set the buttons map
     *
     * @param buttons the map
     */
    public void setButtons(Map buttons) {
        this.buttons = buttons;
    }

    /**
     * Get the buttons map
     *
     * @return the map
     */
    public Map getButtons() {
        return this.buttons;
    }

    /**
     * Set a value for the given field
     *
     * @param key the field name
     * @param value value to set
     */
    public void setButton(String key, Object value) {
        buttons.put(key, value);
    }

    /**
     * Get the value for the given field
     *
     * @param key the field name
     * @return the field value
     */
    public Object getButton(String key) {
        return buttons.get(key);
    }

    /**
     * Return the name of the last button pressed
     * @return the button name
     */
    public String getButton() {
        if (buttons.size() == 0) {
            return "";
        } else {
            return (String) buttons.keySet().iterator().next();
        }
    }

    /**
     * @see ActionForm#validate
     */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map savedBags = (Map) session.getAttribute(Constants.SAVED_BAGS);

        ActionErrors errors = null;

        if (("addToExistingBag".equals(getButton()) || "saveNewBag".equals(getButton()))
            && selectedObjects.length == 0) {
            errors = new ActionErrors();
            errors.add(ActionErrors.GLOBAL_ERROR,
                       new ActionError("errors.savebag.nothingSelected", newBagName));
        }

        if ("saveNewBag".equals(getButton())) {
            if (newBagName.equals("")) {
                errors = new ActionErrors();
                errors.add(ActionErrors.GLOBAL_ERROR,
                           new ActionError("errors.savebag.blank", newBagName));
            } else if (savedBags != null && savedBags.containsKey(newBagName)) {
                errors = new ActionErrors();
                errors.add(ActionErrors.GLOBAL_ERROR,
                           new ActionError("errors.savebag.existing", newBagName));
            }
            return errors;
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
        selectedObjects = new String[] {};
        newBagName = null;
        buttons = new HashMap();
    }
}
