package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
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
import javax.servlet.ServletContext;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.util.XmlUtil;

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
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        String type = request.getParameter("type");
        String name = request.getParameter("name");
        PathQuery query = null;
        
        if (StringUtils.isEmpty(type) || StringUtils.isEmpty(name)) {
            query = (PathQuery) session.getAttribute(Constants.QUERY);
        } else if ("history".equals(type)) {
            query = ((SavedQuery) profile.getHistory().get(name)).getPathQuery();
        } else if ("saved".equals(type)) {
            query = ((SavedQuery) profile.getSavedQueries().get(name)).getPathQuery();
        } else {
            LOG.error("Bad type parameter: " + type);
            return null;
        }
        
        if (query == null) {
            LOG.error("Failed to find query " + name + " of type " + type);
            return null;
        }
        
        response.setContentType("text/plain; charset=us-ascii");
        
        if (StringUtils.isEmpty(request.getParameter("as"))
            || request.getParameter("as").toLowerCase().equals("xml")) {
            String modelName = query.getModel().getName();
            String xml = PathQueryBinding.marshal(query, (name != null ? name : ""), modelName);
            xml = XmlUtil.indentXmlSimple(xml);
            response.getWriter().write(xml);
        } else if (request.getParameter("as").toLowerCase().equals("iql")) {
            Query osQuery = MainHelper.makeQuery(query, profile.getSavedBags());
            String iql = osQuery.toString();
            response.getWriter().println(iql);
        } else if (request.getParameter("as").toLowerCase().equals("sql")) {
            Query osQuery = MainHelper.makeQuery(query, profile.getSavedBags());
            ServletContext servletContext = session.getServletContext();
            ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
            if (os instanceof ObjectStoreInterMineImpl) {
                String sql = ((ObjectStoreInterMineImpl) os).generateSql(osQuery);
                response.getWriter().println(sql);
            } else {
                response.getWriter().println("Not an ObjectStoreInterMineImpl");
            }
        } else {
            response.getWriter().println("Unknown export type: " + request.getParameter("as"));
        }
        
        return null;
    }
}
