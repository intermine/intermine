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
import org.apache.struts.action.ActionMessage;

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
            Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
            Iterator iter = queries.keySet().iterator();
            StringBuffer sb = new StringBuffer();
            while (iter.hasNext()) {
                String queryName = (String) iter.next();
                PathQuery query = (PathQuery) queries.get(queryName);
                queryName = validateQueryName(queryName, profile);
                SessionMethods.saveQuery(session, queryName, query);
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(queryName);
            }
            recordMessage(new ActionMessage("query.imported", sb.toString()), request);
            return mapping.findForward("mymine");
        }
    }
    
    /**
     * Checks that the query name doesn't already exist and returns a numbered
     * name if it does.
     * @param queryName the query name
     * @param profile the user profile
     * @return a validated name for the query
     */
    private String validateQueryName(String queryName, Profile profile) {
        String newQueryName = queryName;
        if (profile.getSavedQueries().containsKey(newQueryName)) {
            int i = 1;
            while (true) {
                String testName = newQueryName + "_" + i;
                if (!profile.getSavedQueries().containsKey(testName)) {
                    return testName;
                }
                i++;
            }
        } else {
            return newQueryName;
        }
    }
}
