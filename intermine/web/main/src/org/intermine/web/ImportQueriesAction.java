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

import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Imports query in XML format and forward user to the query builder.
 *
 * @author Thomas Riley
 */
public class ImportQueriesAction extends InterMineAction
{
    /**
     * @see InterMineAction#execute
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ImportQueriesForm qif = (ImportQueriesForm) form;
        
        Map queries = qif.getQueryMap();
        
        if (queries.size() == 1 && request.getParameter("query_builder") != null
            && request.getParameter("query_builder").equals("yes")) {
            SessionMethods.loadQuery((PathQuery) queries.values().iterator().next(),
                                     session, response);
            return mapping.findForward("query");
        } else {
            Iterator iter = queries.keySet().iterator();
            while (iter.hasNext()) {
                String queryName = (String) iter.next();
                PathQuery query = (PathQuery) queries.get(queryName);
                SessionMethods.saveQuery(session, queryName, query);
            }
            return mapping.findForward("history");
        }
    }
}
