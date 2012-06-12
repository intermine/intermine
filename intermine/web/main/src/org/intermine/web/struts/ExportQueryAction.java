package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2011 FlyMine
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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.SavedQuery;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.objectstore.query.Query;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.util.XmlUtil;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.util.URLGenerator;
import org.intermine.webservice.server.query.result.QueryResultLinkGenerator;

/**
 * Export the current query in XML format.
 *
 * @author Thomas Riley
 */
public class ExportQueryAction extends InterMineAction
{
    protected static final Logger LOG = Logger.getLogger(ExportQueryAction.class);

    /**
     * Method called to export a saved Query.
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    @Override
    public ActionForward execute(ActionMapping mapping,
            ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Profile profile = SessionMethods.getProfile(session);

        String type = request.getParameter("type");
        String name = request.getParameter("name");

        PathQuery query = null;

        if (StringUtils.isEmpty(type) || StringUtils.isEmpty(name)) {
            query = SessionMethods.getQuery(session);
        } else if ("history".equals(type)) {
            SavedQuery sq = profile.getHistory().get(name);

            if (sq == null) {
                recordError(new ActionMessage("errors.query.missing", name), request);
                return mapping.findForward("mymine");
            }

            query = sq.getPathQuery();
        } else if ("saved".equals(type)) {
            SavedQuery sq = profile.getSavedQueries().get(name);

            if (sq == null) {
                recordError(new ActionMessage("errors.query.missing", name), request);
                return mapping.findForward("mymine");
            }

            query = sq.getPathQuery();
        } else {
            LOG.error("Bad type parameter: " + type);
            return null;
        }

        if (query == null) {
            LOG.error("Failed to find query " + name + " of type " + type);
            return null;
        }

        if (query.getView().size() == 0) {
            response.getWriter().write("Invalid query. No fields selected for output.");
            return null;
        }

        response.setContentType("text/plain; charset=utf-8");
        WebResultsExecutor webResultsExecutor = im.getWebResultsExecutor(profile);

        String format;
        if (!StringUtils.isEmpty(request.getParameter("as"))) {
            format = request.getParameter("as").toLowerCase();
        } else {
            format = "xml";
        }
        if ("xml".equals(format)) {
            String xml = getQueryXML(name, query);
            xml = XmlUtil.indentXmlSimple(xml);
            response.getWriter().write(xml);
        } else if ("iql".equals(format)) {
            Query osQuery = webResultsExecutor.makeQuery(query);
            response.getWriter().println(osQuery.toString());
        } else if ("sql".equals(format)) {
            response.getWriter().println(webResultsExecutor.makeSql(query));
        }  else if ("link".equals(format)) {
            String serviceFormat;
            if (request.getParameter("serviceFormat") != null) {
                serviceFormat = request.getParameter("serviceFormat");
            } else {
                serviceFormat = "tab";
            }
            String xml = getQueryXML(name, query);
            String link = new QueryResultLinkGenerator().getLink(new URLGenerator(request)
                    .getPermanentBaseURL(), xml, serviceFormat);
            response.getWriter().write(link);
        } else {
            response.getWriter().println("Unknown export type: " + request.getParameter("as"));
        }

        return null;
    }

    private String getQueryXML(String name, PathQuery query) {
        String modelName = query.getModel().getName();
        return PathQueryBinding.marshal(query, (name != null ? name : ""), modelName,
                PathQuery.USERPROFILE_VERSION);
    }
}
