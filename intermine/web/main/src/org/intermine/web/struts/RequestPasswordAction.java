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

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.util.MailUtils;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.ProfileManager;
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
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
    throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ProfileManager pm = (ProfileManager) servletContext.getAttribute(Constants.PROFILE_MANAGER);
        Map webProperties = (Map) servletContext.getAttribute(Constants.WEB_PROPERTIES);
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
        }

        return mapping.findForward("login");
    }
}
