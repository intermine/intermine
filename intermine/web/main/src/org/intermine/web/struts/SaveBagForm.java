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
 * Form bean for the results table and bag creation form.
 *
 * @author Andrew Varley
 * @author Thomas Riley
 */
public class SaveBagForm extends ActionForm
{

    protected String existingBagName, newBagName;
    protected String[] selectedObjects;
    protected String operationButton;

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
        this.newBagName = newBagName.trim();
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

    /**
     * @return the operationButton
     */
    public String getOperationButton() {
        return operationButton;
    }

    /**
     * @param operationButton the operationButton to set
     */
    public void setOperationButton(String operationButton) {
        this.operationButton = operationButton;
    }
}
