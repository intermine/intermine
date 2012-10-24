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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.ProfileManager;
import org.intermine.web.logic.profile.LoginHandler;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Action to change a user's password with the authority of a token previously emailed to them.
 *
 * @author Matthew Wakeling
 */
public class PasswordResetAction extends LoginHandler
{

    /**
     * Method called when the form is submitted.
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
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
            HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        ProfileManager pm = im.getProfileManager();
        String token = ((PasswordResetForm) form).getToken();
        String password = ((PasswordResetForm) form).getNewpassword();

        session.removeAttribute("passwordResetToken");
        try {
            String username = pm.changePasswordWithToken(token, password);
            Map<String, String> renamedBags = doLogin(request, username, password);
            recordMessage(new ActionMessage("password.changed", username), request);
            recordMessage(new ActionMessage("login.loggedin", username), request);
            if (renamedBags.size() > 0) {
                for (String initName : renamedBags.keySet()) {
                    recordMessage(new ActionMessage("login.renamedbags", initName,
                        renamedBags.get(initName)), request);
                }
            }
        } catch (Exception e) {
            RequestPasswordAction.LOG.warn(e);
            recordError(new ActionMessage("login.invalidemail"), request);
        }
        return mapping.findForward("begin");
    }
}
