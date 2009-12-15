package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.ProfileManager;
import org.intermine.web.logic.session.SessionMethods;

/**
 * @author Xavier Watkins
 *
 */
public class ChangePasswordAction extends InterMineAction
{
    /**
     * Method called when user has finished filling out the 'change password' form
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws an exception
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        ProfileManager pm = im.getProfileManager();
        String username = ((ChangePasswordForm) form).getUsername();
        String password = ((ChangePasswordForm) form).getNewpassword();
        pm.setPassword(username, password);
        try {
            recordMessage(new ActionMessage("password.changed", username), request);
        } catch (Exception e) {
            RequestPasswordAction.LOG.warn(e);
            recordError(new ActionMessage("login.invalidemail"), request);
        }
        return mapping.findForward("mymine");
    }
}
