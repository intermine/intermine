package org.intermine.web;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreQueryDurationException;
import org.intermine.objectstore.query.Query;
import org.intermine.web.results.ChangeResultsForm;
import org.intermine.web.results.PagedResults;
import org.intermine.web.results.TableHelper;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Business logic that interacts with session data. These methods are generally
 * called by actions to perform business logic. Obviously, a lot of the business
 * logic happens in other parts of intermine, called from here, then the users
 * session is updated accordingly.
 *
 * @author  Thomas Riley
 */
public class SessionMethods
{
    /**
     * Executes current query and sets session attributes QUERY_RESULTS and RESULTS_TABLE. If the
     * query fails for some reason, this method returns false and ActionErrors are set on the
     * request.
     *
     * @param session  the http session
     * @param request  the current http request
     * @return         true if query ran successfully, false if an error occured
     */
    public static boolean runQuery(HttpSession session, HttpServletRequest request)
                                                                        throws Exception {
        ServletContext servletContext = session.getServletContext();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);

        PagedResults pr;
        try {
            Query q = MainHelper.makeQuery(query, profile.getSavedBags());
            pr = TableHelper.makeTable(os, q, query.getView());
        } catch (ObjectStoreException e) {
            ActionErrors errors = (ActionErrors) request.getAttribute(Globals.ERROR_KEY);
            if (errors == null) {
                errors = new ActionErrors();
                request.setAttribute(Globals.ERROR_KEY, errors);
            }
            String key = (e instanceof ObjectStoreQueryDurationException)
                ? "errors.query.estimatetimetoolong"
                : "errors.query.objectstoreerror";
            errors.add(ActionErrors.GLOBAL_ERROR, new ActionError(key));
            
            return false;
        }

        session.setAttribute(Constants.QUERY_RESULTS, pr);
        session.setAttribute(Constants.RESULTS_TABLE, pr);
        String queryName = SaveQueryHelper.findNewQueryName(profile.getSavedQueries());
        query.setInfo(pr.getResultsInfo());
        SaveQueryAction.saveQuery(request, queryName, query);
        
        return true;
    }
}
