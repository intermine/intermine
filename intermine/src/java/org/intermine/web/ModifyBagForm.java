package org.intermine.web;

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

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

/**
 * Form bean to used in combining bags
 * @author Mark Woodbridge
 */
public class ModifyBagForm extends ActionForm
{
    protected String[] selectedBags;

    /**
     * Constructor
     */
    public ModifyBagForm() {
        initialise();
    }

    /**
     * Initialiser
     */
   public void initialise() {
        selectedBags = new String[0];
    }

    /**
     * Sets the selected bags
     *
     * @param selectedBags the selected bags
     */
    public void setSelectedBags(String[] selectedBags) {
        this.selectedBags = selectedBags;
    }

    /**
     * Gets the selected bags
     *
     * @return the selected bags
     */
    public String[] getSelectedBags() {
        return selectedBags;
    }

    /**
     * @see ActionForm#validate
     */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errors = null;

//         if (selectedBags.length != 2) {
//             errors = new ActionErrors();
//             errors.add(ActionErrors.GLOBAL_ERROR,
//                        new ActionError("errors.combineBags.tooMany"));
//         }

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
