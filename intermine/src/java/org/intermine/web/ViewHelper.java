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
     * Make a Query object from the information on the session and add it as the "QUERY" attribute
     * to the session.
     *
     * @param request The HTTP request we are currently processing
     */
    public static void makeQuery(HttpServletRequest request) {
        HttpSession session = request.getSession();

        Map qNodes = (Map) session.getAttribute("qNodes");
        ServletContext servletContext = session.getServletContext();
        Model model = (Model) servletContext.getAttribute(Constants.MODEL);

        Map savedQueries = (Map) session.getAttribute(Constants.SAVED_QUERIES);
        String newQueryName = SaveQueryHelper.findNewQueryName(savedQueries);

        Query query = MainHelper.makeQuery(qNodes,
                                           (List) session.getAttribute("view"),
                                           model,
                                           (Map) session.getAttribute(Constants.SAVED_BAGS));
        session.setAttribute("QUERY", query);
    }

    /**
     * Create a ResultsInfo object for the current QUERY and add it as the "resultsInfo" attribute
     * on the request.
     *
     * @param request The HTTP request we are currently processing
     * @return the errors that occured during processing or null if there are no errors
     */
    public static ActionMessages makeEstimate(HttpServletRequest request) {
        HttpSession session = request.getSession();

        Query query = (Query) session.getAttribute(Constants.QUERY);
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);

        try {
            ResultsInfo estimatedResultsInfo = os.estimate(query);

            request.setAttribute("resultsInfo", estimatedResultsInfo);
        } catch (ObjectStoreException e) {
            ActionMessages actionMessages = new ActionMessages();
            ActionError error = new ActionError("errors.query.objectstoreerror");
            return actionMessages;
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
                return actionMessages;
            } else {
                throw e;
            }
        }

        return null;
    }

    /**
     * Retrieve the "QUERY" attribute from the session, run it and save the results by calling
     * TableHelper.makeTable().
     *
     * @param request The HTTP request we are currently processing
     * @return the errors that occured during processing or null if there are no errors
     */
    public static ActionMessages runQuery(HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.removeAttribute(Constants.RESULTS_TABLE);

        Query query = (Query) session.getAttribute(Constants.QUERY);

        try {
            PagedResults pr = TableHelper.makeTable(session, query);
            ResultsInfo resultsInfo = pr.getResults().getInfo();

            Map savedQueries = (Map) session.getAttribute(Constants.SAVED_QUERIES);
            String newQueryName = SaveQueryHelper.findNewQueryName(savedQueries);

            SaveQueryHelper.saveQuery(request, newQueryName, query, resultsInfo);
        } catch (ObjectStoreException e) {
            ActionMessages actionMessages = new ActionMessages();
            ActionError error = new ActionError("errors.query.objectstoreerror");
            return actionMessages;
        }
        return null;
    }
}
