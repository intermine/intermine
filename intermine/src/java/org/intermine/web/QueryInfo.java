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

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.ResultsInfo;

/**
 * A wrapper containing a reference to Query objects and useful information to save with a Query.
 *
 * @author Kim Rutherford
 */

public class QueryInfo
{
    private Query query;
    private ResultsInfo resultsInfo;

    /**
     * Create a new QueryInfo object.
     *
     * @param query the Query to store
     * @param resultsInfo the ResultsInfo object to store.
     */
    public QueryInfo (Query query, ResultsInfo resultsInfo) {
        this.query = query;
        this.resultsInfo = resultsInfo;
    }

    /**
     * Return the Query that was passed to the constructor.
     *
     * @return the Query
     */
    public Query getQuery() {
        return query;
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
