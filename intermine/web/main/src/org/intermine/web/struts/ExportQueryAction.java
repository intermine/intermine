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

import org.intermine.objectstore.query.Query;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.util.XmlUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.query.MainHelper;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.util.URLGenerator;
import org.intermine.webservice.server.query.result.QueryResultLinkGenerator;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

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
    public ActionForward execute(@SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        String type = request.getParameter("type");
        String name = request.getParameter("name");
        PathQuery query = null;

        if (StringUtils.isEmpty(type) || StringUtils.isEmpty(name)) {
            query = (PathQuery) session.getAttribute(Constants.QUERY);
        } else if ("history".equals(type)) {
            query = (profile.getHistory().get(name)).getPathQuery();
        } else if ("saved".equals(type)) {
            query = (profile.getSavedQueries().get(name)).getPathQuery();
        } else {
            LOG.error("Bad type parameter: " + type);
            return null;
        }

        if (query == null) {
            LOG.error("Failed to find query " + name + " of type " + type);
            return null;
        }
        
        if (query.getViewStrings().size() == 0) {
            response.getWriter().write("Invalid query. No fields selected for output.");
            return null;
        }

        response.setContentType("text/plain; charset=us-ascii");

        String format;
        if (!StringUtils.isEmpty(request.getParameter("as"))) {
            format = request.getParameter("as").toLowerCase();
        } else {
            format = "xml";
        }
        if (format.equals("xml")) {
            String xml = getQueryXML(name, query);
            xml = XmlUtil.indentXmlSimple(xml);
            response.getWriter().write(xml);
        } else if (format.equals("iql")) {
            Map<String, InterMineBag> allBags =
                WebUtil.getAllBags(profile.getSavedBags(), SessionMethods
                .getSearchRepository(servletContext));
            Query osQuery = MainHelper.makeQuery(query, allBags, servletContext,
                    null);
            String iql = osQuery.toString();
            response.getWriter().println(iql);
        } else if (format.equals("sql")) {
            Query osQuery = MainHelper.makeQuery(query, profile.getSavedBags(), servletContext,
                    null);
            ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
            if (os instanceof ObjectStoreInterMineImpl) {
                String sql = ((ObjectStoreInterMineImpl) os).generateSql(osQuery);
                response.getWriter().println(sql);
            } else {
                response.getWriter().println("Not an ObjectStoreInterMineImpl");
            }
        }  else if (format.equals("link")) {
            String serviceFormat;
            if (request.getParameter("serviceFormat") != null) {
                serviceFormat = request.getParameter("serviceFormat");
            } else {
                serviceFormat = "tab";
            }
            String xml = getQueryXML(name, query);
            String link = new QueryResultLinkGenerator().getLink(new URLGenerator(request).
                    getPermanentBaseURL(), xml, serviceFormat);
            response.getWriter().write(link);                
        } else {
            response.getWriter().println("Unknown export type: " + request.getParameter("as"));
        }

        return null;
    }

    private String getQueryXML(String name, PathQuery query) {
        String modelName = query.getModel().getName();
        return PathQueryBinding.marshal(query, (name != null ? name : ""), modelName);
    }
}
