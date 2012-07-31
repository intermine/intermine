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

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

/**
 * The form for resetting a password.
 *
 * @author Matthew Wakeling
 */
public class PasswordResetForm extends ActionForm
{
    protected String token, newpassword, newpassword2;

    /**
     * Gets the value of token.
     *
     * @return the value of token
     */
    public String getToken()  {
        return token;
    }

    /**
     * Sets the value of token.
     *
     * @param token value to assign to token
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Gets the value of newpassword.
     *
     * @return the value of newpassword
     */
    public String getNewpassword()  {
        return newpassword;
    }

    /**
     * Sets the value of newpassword.
     *
     * @param newpassword value to assign to newpassword
     */
    public void setNewpassword(String newpassword) {
        this.newpassword = newpassword;
    }

    /**
     * Sets the value of newpassword2.
     *
     * @param newpassword2 the value to assign to newpassword
     */
    public void setNewpassword2(String newpassword2) {
        this.newpassword2 = newpassword2;
    }

    /**
     * Gets the value of newpassword2.
     *
     * @return the value of newpassword2
     */
    public String getNewpassword2() {
        return newpassword2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionErrors validate(@SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") HttpServletRequest request) {
        ActionErrors errors = new ActionErrors();
        if ((newpassword == null || newpassword.length() == 0)
                || (newpassword2 == null || newpassword2.length() == 0)) {
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("password.emptypassword"));
        } else if (!newpassword.equals(newpassword2)) {
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("password.nomatchpass"));
        }
        return errors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset(@SuppressWarnings("unused") ActionMapping mapping,
                      @SuppressWarnings("unused") HttpServletRequest request) {
        newpassword = null;
        newpassword2 = null;
    }
}
