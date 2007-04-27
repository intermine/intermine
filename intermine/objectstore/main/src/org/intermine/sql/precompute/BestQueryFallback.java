package org.intermine.sql.precompute;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.sql.query.Query;
import org.intermine.sql.query.ExplainResult;

/**
 * Gets the database to explain each Query added and keeps hold of the best one so far.
 *
 * @author Andrew Varley
 */
public class BestQueryFallback extends BestQuery
{
    protected Query query;
    protected String queryString;

    /**
     * Constructs a BestQueryFallback
     *
     * @param query a Query - may be null
     * @param queryString a String - may be null
     */
    public BestQueryFallback(Query query, String queryString) {
        this.query = query;
        this.queryString = queryString;
    }

    /**
     * Allows a Query to be added to this tracker.
     *
     * @param q a Query to be added to the tracker
     * @throws BestQueryException always - this BestQuery does not support updates
     */
    @Override
    public void add(@SuppressWarnings("unused") Query q) throws BestQueryException {
        throw new BestQueryException("Cannot add more queries to a BestQueryFallback");
    }

    /**
     * Allows a Query to be added to this tracker.
     *
     * @param q a query String to be added to the tracker
     * @throws BestQueryException always - this BestQuery does not support updates
     */
    @Override
    public void add(@SuppressWarnings("unused") String q) throws BestQueryException {
        throw new BestQueryException("Cannot add more queries to a BestQueryFallback");
    }

    /**
     * Gets the best Query found so far
     *
     * @return the best Query, or null if no Queries added to this object
     */
    @Override
    public Query getBestQuery() {
        return (queryString == null ? query : new Query(queryString));
    }

    /**
     * Gets the best query String found so far
     *
     * @return the best Query, or null if no Queries added to this object
     */
    @Override
    public String getBestQueryString() {
        return (query == null ? queryString : query.getSQLString());
    }

    /**
     * Gets the ExpainResult for the best Query found so far
     *
     * @return null
     */
    public ExplainResult getBestExplainResult() {
        return null;
    }
}

