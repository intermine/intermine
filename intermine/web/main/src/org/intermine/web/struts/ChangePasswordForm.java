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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.ProfileManager;

/**
 * Form bean to represent the inputs needed to change a password from user input.
 * @author Xavier Watkins
 * 
 */
public class ChangePasswordForm extends ActionForm
{
    private String username, oldpassword, newpassword, newpassword2;

    /**
     * The new password
     * @return the newpassword
     */
    public String getNewpassword() {
        return newpassword;
    }

    /**
     * The new password
     * @param newpassword
     *            the newpassword to set
     */
    public void setNewpassword(String newpassword) {
        this.newpassword = newpassword;
    }

    /**
     * New password verification
     * @return the newpassword2
     */
    public String getNewpassword2() {
        return newpassword2;
    }

    /**
     * New password verification
     * @param newpassword2
     *            the newpassword2 to set
     */
    public void setNewpassword2(String newpassword2) {
        this.newpassword2 = newpassword2;
    }

    /**
     * The old password
     * @return the oldpassword
     */
    public String getOldpassword() {
        return oldpassword;
    }

    /**
     * The old password
     * @param oldpassword
     *            the oldpassword to set
     */
    public void setOldpassword(String oldpassword) {
        this.oldpassword = oldpassword;
    }

    /**
     * The username
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * The username
     * @param username
     *            the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @see ActionForm#validate
     */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ProfileManager pm = (ProfileManager) servletContext.getAttribute(Constants.PROFILE_MANAGER);
        ActionErrors errors = new ActionErrors();
        if (username == null || username.length() == 0) {
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("password.emptyusername"));
        } else if ((oldpassword == null || oldpassword.length() == 0)
                || (newpassword == null || newpassword.length() == 0)
                || (newpassword2 == null || newpassword2.length() == 0)) {
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("password.emptypassword"));
        } else if (!newpassword.equals(newpassword2)) {
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("password.nomatchpass"));
        } else if (!pm.hasProfile(username)) {
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("password.usernotexist",
                    username));
        } else if (!pm.validPassword(username, oldpassword)) {
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("password.wrongpass"));
        }

        return errors;
    }

    /**
     * @see ActionForm#reset
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        username = null;
        oldpassword = null;
        newpassword = null;
        newpassword2 = null;
    }
}
