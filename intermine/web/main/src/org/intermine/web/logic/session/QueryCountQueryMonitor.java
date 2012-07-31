package org.intermine.web.logic.session;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.query.Query;

import org.intermine.web.logic.query.QueryMonitorTimeout;

/**
 * A QueryMonitor that maintains a timeout and is used when counting the number of rows a query
 * returns.
 * @author Kim Rutherford
 */
public class QueryCountQueryMonitor extends QueryMonitorTimeout
{

    private final Query query;
    private int count = -1;

    /**
     * Construct a new instance of QueryCountQueryMonitor.
     *
     * @param n the number of milliseconds to timeout after
     * @param query the Query to find the row count of
     */
    public QueryCountQueryMonitor(int n, Query query) {
        super(n);
        this.query = query;
    }

    /**
     * Set the query count/size.
     * @param count the number of rows returned by the query
     */
    public void setCount(int count) {
        this.count = count;
    }

    /**
     * Return the Query that was passed to the constructor.
     * @return the query
     */
    public final Query getQuery() {
        return query;
    }

    /**
     * Return the count set with setCount().
     * @return the count
     */
    public final int getCount() {
        return count;
    }


}
