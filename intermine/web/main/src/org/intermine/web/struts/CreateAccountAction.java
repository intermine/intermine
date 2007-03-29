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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.LoginHandler;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;

/**
 * @author Xavier Watkins
 *
 */
public class CreateAccountAction extends LoginHandler
{
    /**
     * Method called when user has finished updating a constraint
     *
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws an exception
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ProfileManager pm = (ProfileManager) servletContext.getAttribute(Constants.PROFILE_MANAGER);
        String username = ((CreateAccountForm) form).getUsername();
        String password = ((CreateAccountForm) form).getPassword();
        pm.createProfile(new Profile(pm, username, null, password, new HashMap(), new HashMap(),
                new HashMap()));
        Map webProperties = (Map) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        RequestPasswordAction.email(username, password, webProperties);
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

        doLogin(servletContext, request, response, session, pm, username, password);

        return mapping.findForward("mymine");
    }

}
