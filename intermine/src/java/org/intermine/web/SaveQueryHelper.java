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
import java.util.LinkedHashMap;
import java.util.IdentityHashMap;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.ResultsInfo;

/**
 * Helper methods for SaveQueryAction.
 *
 * @author Kim Rutherford
 */

public class SaveQueryHelper
{
    /**
     * Save a Query and it's name in session in the SAVED_QUERIES and SAVED_QUERIES_INVERSE Maps.
     *
     * @param request The HTTP request we are processing
     * @param queryName the name to use as the key when saving into the SAVED_QUERIES Map and as the
     * value when saving into the SAVED_QUERIES_INVERSE Map
     * @param query the Query to use as the key when saving into the SAVED_QUERIES_INVERSE Map and
     * as the value when saving into the SAVED_QUERIES Map
     * @param resultsInfo the ResultsInfo used to save statistics of the query
     */
    public static void saveQuery(HttpServletRequest request,
                                 String queryName, Query query, ResultsInfo resultsInfo) {
        HttpSession session = request.getSession();

        createSavedQueryMaps(session);
        
        Map savedQueriesInverse = (Map) session.getAttribute(Constants.SAVED_QUERIES_INVERSE);
        Map savedQueries = (Map) session.getAttribute(Constants.SAVED_QUERIES);
        Map queryInfoMap = (Map) session.getAttribute(Constants.QUERY_INFO_MAP);

        savedQueries.put(queryName, query);
        savedQueriesInverse.put(query, queryName);
        queryInfoMap.put(query, new QueryInfo(query, resultsInfo));

        request.setAttribute(Constants.SAVED_QUERY_NAME, queryName);
    }


    private static final String QUERY_NAME_PREFIX = "query_";

    /**
     * Return a query name that isn't currently in use.
     *
     * @param savedQueries the Map of current saved queries
     * @return the new query name
     */
    public static String findNewQueryName(Map savedQueries) {
        int i = 1;

        while (true) {
            String testName = QUERY_NAME_PREFIX + i;

            if (savedQueries == null || savedQueries.get(testName) == null) {
                return testName;
            }

            i++;
        }
    }

    /**
     * Add (if necessary) the session attributes that contain the saved queries.
     *
     * @param session the session to add the attributes to
     */
    public static void createSavedQueryMaps(HttpSession session) {
        Map savedQueries = (Map) session.getAttribute(Constants.SAVED_QUERIES);
        if (savedQueries == null) {
            savedQueries = new LinkedHashMap();
            session.setAttribute(Constants.SAVED_QUERIES, savedQueries);
        }

        Map savedQueriesInverse = (Map) session.getAttribute(Constants.SAVED_QUERIES_INVERSE);
        if (savedQueriesInverse == null) {
            savedQueriesInverse = new IdentityHashMap();
            session.setAttribute(Constants.SAVED_QUERIES_INVERSE, savedQueriesInverse);
        }

        Map queryInfoMap = (Map) session.getAttribute(Constants.QUERY_INFO_MAP);
        if (queryInfoMap == null) {
            queryInfoMap = new IdentityHashMap();
            session.setAttribute(Constants.QUERY_INFO_MAP, queryInfoMap);
        }
    }
}
