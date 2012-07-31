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

import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.ProfileManager;
import org.intermine.util.MailUtils;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.util.URLGenerator;

/**
 * Action to handle button presses RequestPasswordForm
 *
 * @author Mark Woodbridge
 */
public class RequestPasswordAction extends InterMineAction
{
    protected static final Logger LOG = Logger.getLogger(RequestPasswordAction.class);

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
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());

        ProfileManager pm = im.getProfileManager();

        ServletContext servletContext = request.getSession().getServletContext();
        Properties webProperties = SessionMethods.getWebProperties(servletContext);
        String username = ((RequestPasswordForm) form).getUsername();

        if (pm.hasProfile(username)) {
            try {
                String token = pm.createPasswordChangeToken(username);
                MailUtils.emailPasswordToken(username, new URLGenerator(request)
                        .getPermanentBaseURL() + "/passwordReset.do?token=" + token,
                        webProperties);
                recordMessage(new ActionMessage("login.emailed", username), request);
            } catch (Exception e) {
                RequestPasswordAction.LOG.warn(e);
                recordError(new ActionMessage("login.mailnotsent", e), request);
            }
        } else {
            recordError(new ActionMessage("login.emptyusername"), request);
        }
        return mapping.findForward("login");
    }
}
