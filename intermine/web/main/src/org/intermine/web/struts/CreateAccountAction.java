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

import java.util.HashMap;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.util.MailUtils;
import org.intermine.web.logic.profile.LoginHandler;
import org.intermine.web.logic.session.SessionMethods;

/**
 * @author Xavier Watkins
 */
public class CreateAccountAction extends LoginHandler
{
    /**
     * Method called when user has finished updating a constraint.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws an exception
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        ProfileManager pm = im.getProfileManager();
        String username = ((CreateAccountForm) form).getUsername();
        String password = ((CreateAccountForm) form).getPassword();
        pm.createNewProfile(username, password);
        Properties webProperties = SessionMethods.getWebProperties(session.getServletContext());
        try {
            MailUtils.welcome(username, webProperties);
            if (((CreateAccountForm) form).getMailinglist()
                && webProperties.getProperty("mail.mailing-list") != null
                && webProperties.getProperty("mail.mailing-list").length() > 0) {
                MailUtils.subscribe(username, webProperties);
            }
            SessionMethods.recordMessage("You have successfully created an account, and logged in.",
                    session);
        } catch (Exception e) {
            SessionMethods.recordError("Failed to send confirmation email", session);
        }

        /*
         * This code generates an MD5 key for the given username which is then
         * encoded in Hexadecimal. This could later be used for account
         * activation.
         *
         * try { MessageDigest md5 = MessageDigest.getInstance("MD5"); byte[]
         * buffer = username.getBytes(); md5.update(buffer); byte[] array =
         * md5.digest(); String encoded = HexBin.encode(array); } catch
         * (NoSuchAlgorithmException e) { }
         */
        doLogin(request, username, password);

        if (session.getAttribute("returnTo") != null) {
            return new ActionForward(session.getAttribute("returnTo").toString());
        }

        return new ActionForward("/begin.do");
    }
}
