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

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletContext;

import org.apache.struts.action.ActionMessages;
import org.apache.struts.action.ActionError;

import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreQueryDurationException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.web.results.TableHelper;
import org.intermine.web.results.PagedResults;

/**
 * Helper methods for the ViewChange class.
 *
 * @author Kim Rutherford
 */
public abstract class ViewHelper
{
    /**
     * Create a ResultsInfo object for a query on the request and add it to the request
     *
     * @param request The HTTP request we are currently processing
     * @return the errors that occured during processing or null if there are no errors
     */
    public static ResultsInfo makeEstimate(HttpServletRequest request) {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);

        ResultsInfo resultsInfo = null;

        try {
            resultsInfo = os.estimate(makeQuery(request));
        } catch (ObjectStoreException e) {
            ActionMessages actionMessages = new ActionMessages();
            ActionError error = new ActionError("errors.query.objectstoreerror");
            actionMessages.add(ActionMessages.GLOBAL_MESSAGE, error);
        } catch (RuntimeException e) {
            if (e.getCause() != null && e.getCause() instanceof ObjectStoreException) {
                ActionMessages actionMessages = new ActionMessages();
                ActionError error;

                if (e.getCause() instanceof ObjectStoreQueryDurationException) {
                    error = new ActionError("errors.query.estimatetimetoolong");
                } else {
                    error = new ActionError("errors.query.objectstoreerror");
                }

                actionMessages.add(ActionMessages.GLOBAL_MESSAGE, error);
            } else {
                throw e;
            }
        }

        return resultsInfo;
    }

    /**
     * Retrieve the query from the request, run it and save the results by calling
     * TableHelper.makeTable().
     *
     * @param request The HTTP request we are currently processing
     * @return the errors that occured during processing or null if there are no errors
     */
    public static ActionMessages runQuery(HttpServletRequest request) {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        List view = (List) session.getAttribute(Constants.VIEW);
        Map qNodes = (Map) session.getAttribute(Constants.QUERY);
        Map savedQueries = (Map) session.getAttribute(Constants.SAVED_QUERIES);

        PagedResults pr = null;
        try {
            pr = TableHelper.makeTable(os, makeQuery(request), view);
            String queryName = SaveQueryHelper.findNewQueryName(savedQueries);
            ResultsInfo resultsInfo = pr.getResults().getInfo();
            SaveQueryAction.saveQuery(request, queryName, qNodes, view, resultsInfo);
            request.setAttribute("savedQueryName", queryName);
        } catch (ObjectStoreException e) {
            ActionMessages actionMessages = new ActionMessages();
            ActionError error = new ActionError("errors.query.objectstoreerror");
            return actionMessages;
        }
        session.setAttribute(Constants.RESULTS_TABLE, pr);
        
        return null;
    }

    /**
     * Invoke MainHelper#makeQuery using parameters from Session and ServletContext
     * @param request The HTTP request we are currently processing
     * @return Query the returned Query
     */
    public static Query makeQuery(HttpServletRequest request) {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();

        Map qNodes = (Map) session.getAttribute(Constants.QUERY);
        List view = (List) session.getAttribute(Constants.VIEW);
        Model model = (Model) servletContext.getAttribute(Constants.MODEL);
        Map savedBags = (Map) session.getAttribute(Constants.SAVED_BAGS);

        return MainHelper.makeQuery(qNodes, view, model, savedBags);
    }
}
