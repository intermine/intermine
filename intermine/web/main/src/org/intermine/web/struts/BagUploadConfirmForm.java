package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2014 FlyMine
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
 * Form for the bagUploadConfirm page.
 * @author Kim Rutherford
 */
public class BagUploadConfirmForm extends ActionForm
{
    private String newBagName;
    private String bagType;
    private String matchIDs;
    private String[] selectedObjects;
    private String extraFieldValue;

    /**
     * Constructor
     */
    public BagUploadConfirmForm() {
        initialise();
    }

    /**
     * Initialise the form.
     */
    public void initialise() {
        newBagName = "";
        matchIDs = "";
        selectedObjects = new String[] {};
    }

    /**
     * Set the bag name.
     * @param name the bag name
     */
    public void setNewBagName(String name) {
        newBagName = name.trim();
    }

    /**
     * Get the bag name.
     * @return the bag name
     */
    public String getNewBagName() {
        return newBagName;
    }


    /**
     * Sets the ids of the selected objects - ie. those that should be included in the new bag
     * @param selectedObjects the selected objects
     */
    public void setSelectedObjects(String[] selectedObjects) {
        this.selectedObjects = selectedObjects;
    }

    /**
     * Gets the ids of the selected objects
     * @return the selected objects
     */
    public String[] getSelectedObjects() {
        return selectedObjects;
    }

    /**
     * Get the encoded match ids (hidden form field).
     * @return the match IDs
     */
    public String getMatchIDs() {
        return matchIDs;
    }

    /**
     * Set the encoded match ids.
     * @param matchIDs the encoded match ids
     */
    public void setMatchIDs(String matchIDs) {
        this.matchIDs = matchIDs;
    }

    /**
     * Get the bag type - hidden form value.
     * @return the bag type
     */
    public String getBagType() {
        return bagType;
    }

    /**
     * Set the bag type
     * @param bagType the new bag type
     */
    public void setBagType(String bagType) {
        this.bagType = bagType;
    }

    /**
     * Set the extra field value - the optional constraint on the bag upload page
     * @return the extra field value
     */
    public String getExtraFieldValue() {
        return extraFieldValue;
    }

    /**
     * Set the extra field value
     * @param extraFieldValue the extra field value
     */
    public void setExtraFieldValue(String extraFieldValue) {
        this.extraFieldValue = extraFieldValue;
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
