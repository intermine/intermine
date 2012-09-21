package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2012 FlyMine
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
 * Form bean used in user admin page
 * @author Daniela Butano
 */
public class ModifySuperUserForm extends ActionForm
{
    protected String[] superUsers;

    /**
     * Constructor
     */
    public ModifySuperUserForm() {
        super();
        initialise();
    }

    /**
     * Initialiser
     */
    public void initialise() {
        superUsers = new String[0];
    }

    /**
     * Sets the super users
     *
     * @param superUsers the selected super users
     */
    public void setSuperUsers(String[] superUsers) {
        this.superUsers = superUsers;
    }

    /**
     * Get the super users
     *
     * @return the map
     */
    public String[] getSuperUsers() {
        return superUsers;
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
