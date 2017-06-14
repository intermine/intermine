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

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

/**
 * @author Xavier Watkins
 *
 */
public class ModifyBagDetailsForm extends ActionForm
{
    protected String[] selectedElements;
    protected String bagName, existingBagName;
    protected String extraFieldValue;

    /**
     * Constructor
     */
    public ModifyBagDetailsForm() {
        initialise();
    }

    /**
     * Initialiser
     */
    public void initialise() {
        selectedElements = new String[0];
        bagName = "";
    }

    /**
     * Get the value of bagName
     * @return the value of bagName
     */
    public String getBagName() {
        return bagName;
    }

    /**
     * Set the value of bagName
     * @param bagName the bagName
     */
    public void setBagName(String bagName) {
        this.bagName = bagName;
    }

    /**
     * Get a String array of selected bag elements
     * @return a String array
     */
    public String[] getSelectedElements() {
        return selectedElements;
    }

    /**
     * Set the list of selectedElements
     * @param selectedElements a String array
     */
    public void setSelectedElements(String[] selectedElements) {
        this.selectedElements = selectedElements;
    }

    /**
     * @return the extraFieldValue
     */
    public String getExtraFieldValue() {
        return extraFieldValue;
    }

    /**
     * @param extraFieldValue the extraFieldValue to set
     */
    public void setExtraFieldValue(String extraFieldValue) {
        this.extraFieldValue = extraFieldValue;
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
