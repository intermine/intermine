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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.ProfileManager;

/**
 * Form bean to represent the inputs to create a new user account
 * 
 * @author Xavier Watkins
 * 
 */
public class CreateAccountForm extends ActionForm
{
    protected String username, password, password2;

    /**
     * @return password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password
     *            The password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username
     *            The username/email
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return password2
     */
    public String getPassword2() {
        return password2;
    }

    /**
     * @param password2
     *            Password verification
     */
    public void setPassword2(String password2) {
        this.password2 = password2;
    }

    /**
     * {@inheritDoc}
     */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ProfileManager pm = (ProfileManager) servletContext.getAttribute(Constants.PROFILE_MANAGER);
        ActionErrors errors = new ActionErrors();
        Pattern p = Pattern.compile(".+@.+\\.[a-z]+");
        Matcher m = p.matcher(username);
        if (username == null || username.length() == 0) {
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage(
                    "createAccount.emptyusername"));
        } else if (password == null || password.length() == 0) {
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage(
                    "createAccount.emptypassword"));
        } else if (!password.equals(password2)) {
            errors.add(ActionMessages.GLOBAL_MESSAGE,
                    new ActionMessage("createAccount.nomatchpass"));
        } else if (pm.hasProfile(username)) {
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("createAccount.userexists",
                    username));
        } else if (!m.matches()) {
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage(
                    "createAccount.nonvalidemail", username));
        }
        return errors;
    }

    /**
     * {@inheritDoc}
     */
    public void reset(@SuppressWarnings("unused") ActionMapping mapping, 
                      @SuppressWarnings("unused") HttpServletRequest request) {
        username = null;
        password = null;
        password2 = null;
    }

}
