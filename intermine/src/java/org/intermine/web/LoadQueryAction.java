package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;
import java.util.ArrayList;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.actions.DispatchAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.intermine.objectstore.ObjectStore;

/**
 * Implementation of <strong>Action</strong> that sets the current Query for
 * the session from a saved Query.
 *
 * @author Kim Rutherford
 */
public class LoadQueryAction extends DispatchAction
{
    /**
     * Load a query ie. take a query from the exampleQueries and make it the current one
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward load(ActionMapping mapping,
                              ActionForm form,
                              HttpServletRequest request,
                              HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Map exampleQueries = (Map) servletContext.getAttribute(Constants.EXAMPLE_QUERIES);
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        String queryName = request.getParameter("name");

        if (exampleQueries != null && exampleQueries.containsKey(queryName)) {
            QueryInfo queryInfo = (QueryInfo) exampleQueries.get(queryName);

            session.setAttribute(Constants.QUERY,
                                 SaveQueryHelper.clone(queryInfo.getQuery(), os.getModel()));
            session.setAttribute(Constants.VIEW, new ArrayList(queryInfo.getView()));
            session.removeAttribute("path");
            session.removeAttribute("prefix");

            return mapping.findForward("query");
        } else {
            return mapping.findForward("error");
        }
    }
}
