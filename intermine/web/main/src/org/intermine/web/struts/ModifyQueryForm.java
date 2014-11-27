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

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

/**
 * Form bean to used in combining queries
 * @author Mark Woodbridge
 */
public class ModifyQueryForm extends ActionForm
{
    protected String[] selectedQueries;

    /**
     * Constructor
     */
    public ModifyQueryForm() {
        initialise();
    }

    /**
     * Initialiser
     */
    public void initialise() {
        selectedQueries = new String[0];
    }

    /**
     * Sets the selected queries
     *
     * @param selectedQueries the selected queries
     */
    public void setSelectedQueries(String[] selectedQueries) {
        this.selectedQueries = selectedQueries;
    }

    /**
     * Gets the selected queries
     *
     * @return the selected queries
     */
    public String[] getSelectedQueries() {
        return selectedQueries;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errors = null;
        if (selectedQueries.length == 0) {
            errors = new ActionErrors();
            errors.add(ActionMessages.GLOBAL_MESSAGE,
                       new ActionMessage("errors.modifyQuery.noselect"));
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
    public void reset(ActionMapping mapping,
                      HttpServletRequest request) {
        initialise();
    }
}
