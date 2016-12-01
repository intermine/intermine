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
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.ProfileManager;
import org.intermine.web.logic.session.SessionMethods;

/**
 * The main form, using for editing constraints
 * @author Mark Woodbridge
 */
public class LoginForm extends ActionForm
{
    protected String username, password, returnToString;

    /**
     * Gets the value of username
     *
     * The user name is <em>always</em> returned in <strong>LOWER CASE</strong>.
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
     * Gets the value of password
     *
     * @return the value of password
     */
    public String getPassword()  {
        return password;
    }

    /**
     * Sets the value of password
     *
     * @param password value to assign to password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Sets the URL to return to if the login is successful.
     * @param returnToString the path to return to after log in
     */
    public void setReturnToString(String returnToString) {
        this.returnToString = returnToString;
    }

    /**
     * Return the returnToString set by setReturnToURL().
     * @return the the path to return to after log in
     */
    public String getReturnToString() {
        return returnToString;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        ProfileManager pm = im.getProfileManager();

        ActionErrors errors = new ActionErrors();

        if ("".equals(username)) {
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("login.emptyusername"));
        } else {
            if (pm.hasProfile(username)) {
                if (!pm.validPassword(username, password)) {
                    errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("login.badlogin"));
                }
            } else {
                errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("login.badlogin"));
            }
        }
        return errors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        username = null;
        password = null;
    }
}
