package org.intermine.sql.precompute;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.SQLException;

import org.intermine.sql.query.Query;

/**
 * This object is a BestQuery tracker that encloses all added queries into a surrounding query as a
 * subquery, and passes them onto another BestQuery tracker.
 *
 * @author Matthew Wakeling
 */
public class EncloseSubqueryBestQuery extends BestQuery
{
    BestQuery bestQuery;
    String beginning;
    String end;

    /**
     * Constructor.
     *
     * @param bestQuery another BestQuery object to delegate to
     * @param beginning the String to add to the beginning of all queries
     * @param end the String to add to the end of all queries
     */
    public EncloseSubqueryBestQuery(BestQuery bestQuery, String beginning, String end) {
        this.bestQuery = bestQuery;
        this.beginning = beginning;
        this.end = end;
    }

    /**
     * Allows a Query to be added to this tracker.
     *
     * @param q a Query to be added to the tracker
     * @throws BestQueryException when adding should stop
     * @throws SQLException if error occurs in the underlying database
     */
    @Override
    public void add(Query q) throws BestQueryException, SQLException {
        bestQuery.add(beginning + q.getSQLString() + end);
    }

    /**
     * Allows a Query to be added to this tracker.
     *
     * @param q a query String to be added to the tracker
     * @throws BestQueryException when adding should stop
     * @throws SQLException if error occurs in the underlying database
     */
    @Override
    public void add(String q) throws BestQueryException, SQLException {
        bestQuery.add(beginning + q + end);
    }

    /**
     * Gets the best Query found so far
     *
     * @return the best Query, or null if no Queries added to this object
     * @throws SQLException if error occurs in the underlying database
     */
    @Override
    public Query getBestQuery() throws SQLException {
        return bestQuery.getBestQuery();
    }

    /**
     * Gets the best query String found so far
     *
     * @return the best Query, or null if no Queries added to this object
     * @throws SQLException if error occurs in the underlying database
     */
    @Override
    public String getBestQueryString() throws SQLException {
        return bestQuery.getBestQueryString();
    }
}
