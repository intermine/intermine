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

import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
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
     * @throws ObjectStoreException if an error occurs accessing the ObjectStore
     */
    public static ResultsInfo makeEstimate(HttpServletRequest request) throws ObjectStoreException {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        
        return os.estimate(makeQuery(request));
    }

    /**
     * Retrieve the query from the request, run it and save the results by calling
     * TableHelper.makeTable().
     *
     * @param request The HTTP request we are currently processing
     * @return the results of running the query
     * @throws ObjectStoreException if an error occurs accessing the ObjectStore
     */
    public static PagedResults runQuery(HttpServletRequest request) throws ObjectStoreException {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        List view = (List) session.getAttribute(Constants.VIEW);
        Map qNodes = (Map) session.getAttribute(Constants.QUERY);
        Map savedQueries = (Map) session.getAttribute(Constants.SAVED_QUERIES);

        Query q = makeQuery(request);
        PagedResults pr = TableHelper.makeTable(os, q, view);
        String queryName = SaveQueryHelper.findNewQueryName(savedQueries);
        ResultsInfo resultsInfo = pr.getResultsInfo();
        SaveQueryAction.saveQuery(request, queryName, qNodes, view, resultsInfo);
        return pr;
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
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Model model = (Model) os.getModel();
        Map savedBags = (Map) session.getAttribute(Constants.SAVED_BAGS);

        return MainHelper.makeQuery(qNodes, view, model, savedBags);
    }
}
