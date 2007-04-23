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

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

/**
 * Form used to request a password
 * @author Mark Woodbridge
 */
public class RequestPasswordForm extends ActionForm
{
    protected String username, password;

    /**
     * Gets the value of username
     *
     * @return the value of username
     */
    public String getUsername()  {
        return username;
    }

    /**
     * Sets the value of username
     *
     * @param username value to assign to username
     */
    public void setUsername(String username) {
        this.username = username.toLowerCase();
    }

    /**
     * {@inheritDoc}
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        username = null;
    }
}
