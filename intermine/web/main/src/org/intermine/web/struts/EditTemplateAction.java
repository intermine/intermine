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
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.template.TemplateBuildState;
import org.intermine.web.logic.template.TemplateHelper;
import org.intermine.web.logic.template.TemplateQuery;

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
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        String queryName = request.getParameter("name");
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        TemplateQuery template = TemplateHelper.findTemplate(servletContext, session,
                profile.getUsername(), queryName, TemplateHelper.ALL_TEMPLATE);
        
        PathQuery queryClone = (PathQuery) template.clone();
        SessionMethods.loadQuery(queryClone, session, response);
        session.setAttribute(Constants.TEMPLATE_BUILD_STATE, new TemplateBuildState(template));
        return mapping.findForward("query");
    }
}
