package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.template.ApiTemplate;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Make some change to a user template.
 *
 * @author Thomas Riley
 */
public class UserTemplateAction extends InterMineDispatchAction
{
    /**
     * Delete a template query.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward delete(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        String templateName = request.getParameter("name");
        Profile profile = SessionMethods.getProfile(session);

        ApiTemplate template = profile.getSavedTemplates().get(templateName);
        if (template != null) {
            recordMessage(new ActionMessage("templateList.deleted", templateName), request);
            InterMineAPI im = SessionMethods.getInterMineAPI(session);
            profile.deleteTemplate(templateName, im.getTrackerDelegate(), true);
        } else {
            recordError(new ActionMessage("errors.template.nosuchtemplate"), request);
        }

        return new ForwardParameters(mapping.findForward("begin"))
            .addParameter("subtab", "templates").forward();
    }
}
