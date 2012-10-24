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
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.SavedQuery;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.TemplateQuery;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Action to display the query builder (if there is a current query) or redirect to
 * project.sitePrefix.
 * @author Tom Riley
 */
public class CurrentQueryAction extends InterMineAction
{
    /**
     * Either display the query builder or redirect to project.sitePrefix.
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
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        PathQuery query = SessionMethods.getQuery(session);
        boolean showTemplate = (request.getParameter("showTemplate") != null);

        if (query == null) {
            return new ForwardParameters(getWebProperties(request)
                    .getProperty("project.sitePrefix"), true).forward();
        }

        if (query instanceof TemplateQuery && showTemplate) {
            TemplateQuery template = (TemplateQuery) query;
            Profile profile = SessionMethods.getProfile(session);
            String temporaryName = null;
            for (SavedQuery sq : profile.getHistory().values()) {
                if (sq.getPathQuery() instanceof TemplateQuery
                    && ((TemplateQuery) sq.getPathQuery()).getName().equals(template.getName())) {
                    temporaryName = sq.getName();
                }
            }
            if (temporaryName != null) {
                return new ForwardParameters(mapping.findForward("template"))
                    .addParameter("loadModifiedTemplate", "true")
                    .addParameter("name", template.getName())
                    .addParameter("savedQueryName", temporaryName).forward();
            }
            return new ForwardParameters(mapping.findForward("template"))
                .addParameter("name", template.getName()).forward();
        }
        if (!(query instanceof TemplateQuery)) {
            session.removeAttribute(Constants.EDITING_TEMPLATE);
            session.removeAttribute(Constants.NEW_TEMPLATE);
        }
        return mapping.findForward("query");
    }
}
