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
import java.util.List;

import org.intermine.objectstore.query.ResultsInfo;

/**
 * A wrapper containing a reference to Query objects and useful information to save with a Query.
 *
 * @author Kim Rutherford
 */
public class QueryInfo
{
    private Map query;
    private List view;
    private ResultsInfo resultsInfo;

    /**
     * Create a new QueryInfo object.
     *
     * @param query the Query to store
     * @param view the paths in the SELECT list for the query
     * @param resultsInfo the ResultsInfo object to store.
     */
    public QueryInfo(Map query, List view, ResultsInfo resultsInfo) {
        this.query = query;
        this.view = view;
        this.resultsInfo = resultsInfo;
    }

    /**
     * Return the Query that was passed to the constructor.
     *
     * @return the Query
     */
    public Map getQuery() {
        return query;
    }

    /**
     * Return the value of view
     * @return the value of view
     */
    public List getView() {
        return view;
    }

    /**
     * Return the ResultsInfo that was passed to the constructor.
     *
     * @return the ResultsInfo
     */
    public ResultsInfo getResultsInfo () {
        return resultsInfo;
    }
}
