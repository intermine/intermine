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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.StringWriter;
import java.io.PrintWriter;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionErrors;

import org.apache.log4j.Logger;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreQueryDurationException;
import org.intermine.objectstore.query.Query;

import org.intermine.web.results.PagedResults;
import org.intermine.web.results.TableHelper;

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
    protected static final Logger LOG = Logger.getLogger(SessionMethods.class);

    /**
     * Executes current query and sets session attributes QUERY_RESULTS and RESULTS_TABLE. If the
     * query fails for some reason, this method returns false and ActionErrors are set on the
     * request.
     *
     * @param session   the http session
     * @param request   the current http request
     * @param saveQuery if true, query will be saved automatically
     * @return  true if query ran successfully, false if an error occured
     * @throws  Exception if getting results info from paged results fails
     */
    public static boolean runQuery(HttpSession session, HttpServletRequest request, boolean saveQuery)
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
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage(key));

            // put stack trace in the log
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            LOG.error(sw.toString());
            
            return false;
        }

        session.setAttribute(Constants.QUERY_RESULTS, pr);
        session.setAttribute(Constants.RESULTS_TABLE, pr);
        
        if (saveQuery) {
            String queryName = SaveQueryHelper.findNewQueryName(profile.getSavedQueries());
            query.setInfo(pr.getResultsInfo());
            saveQuery(request, queryName, query);

            ActionMessages messages = (ActionMessages) request.getAttribute(Globals.MESSAGE_KEY);
            if (messages == null) {
                messages = new ActionMessages();
            }
            messages.add("saveQuery", new ActionMessage("saveQuery.message", queryName));
            request.setAttribute(Globals.MESSAGE_KEY, messages);
        }
        
        return true;
    }
    
    /**
     * Load a query into the session, cloning to avoid modifying the original
     * @param query the query
     * @param session the session
     */
    public static void loadQuery(PathQuery query, HttpSession session) {
        session.setAttribute(Constants.QUERY, query.clone());
        //at the moment we can only load queries that have saved using the webapp
        //this is because the webapp satisfies our (dumb) assumption that the view list is not empty
        String path = (String) query.getView().iterator().next();
        if (path.indexOf(".") != -1) {
            path = path.substring(0, path.indexOf("."));
        }
        session.setAttribute("path", path);
        session.removeAttribute("prefix");
    }
    
    /**
     * Save a query in the Map on the session, and clone it to allow further editing
     * @param request The HTTP request we are processing
     * @param queryName the name to save the query under
     * @param query the PathQuery
     */
    public static void saveQuery(HttpServletRequest request,
                                 String queryName,
                                 PathQuery query) {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);

        profile.saveQuery(queryName, query);
        session.setAttribute(Constants.QUERY, query.clone());
    }
}
