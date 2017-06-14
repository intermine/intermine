package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
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
import org.intermine.pathquery.PathQuery;
import org.intermine.api.template.TemplateManager;
import org.intermine.template.TemplateQuery;
import org.intermine.metadata.StringUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Action to edit a user template query. The action expect a <code>name</code>
 * parameter identifying the template to edit.
 *
 * @author Thomas Riley
 */
public class EditTemplateAction extends InterMineAction
{
    /**
     * Process the specified HTTP request, and create the corresponding HTTP
     * response (or forward to another web component that will create it).
     * Return an <code>ActionForward</code> instance describing where and how
     * control should be forwarded, or <code>null</code> if the response has
     * already been completed.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);

        String queryName = request.getParameter("name");
        Profile profile = SessionMethods.getProfile(session);

        TemplateManager templateManager = im.getTemplateManager();
        TemplateQuery template = templateManager.getUserOrGlobalTemplate(profile, queryName);

        if (template == null) {
            recordError(new ActionMessage("errors.template.missing", queryName), request);
            return mapping.findForward("mymine");
        }

        SessionMethods.loadQuery(template, session, response);
        session.setAttribute(Constants.EDITING_TEMPLATE, Boolean.TRUE);
        PathQuery sessionQuery = SessionMethods.getQuery(session);
        if (!sessionQuery.isValid()) {
            recordError(new ActionMessage("errors.template.badtemplate",
                    StringUtil.prettyList(sessionQuery.verifyQuery())), request);
        }

        return mapping.findForward("query");
    }
}
