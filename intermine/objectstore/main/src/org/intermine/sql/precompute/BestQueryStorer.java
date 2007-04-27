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

import java.util.Set;
import org.intermine.sql.query.Query;
import org.intermine.util.ConsistentSet;

/**
 * Stores each query added.
 *
 * @author Andrew Varley
 */
public class BestQueryStorer extends BestQuery
{
    protected Set queries = new ConsistentSet();

    /**
     * Constructs a BestQueryStorer
     *
     */
    public BestQueryStorer() {
        super();
    }

    /**
     * Allows a Query to be added to this tracker.
     *
     * @param q a Query to be added to the tracker
     */
    @Override
    public void add(Query q) {
        if (q == null) {
            throw new NullPointerException("Cannot add null queries to a BestQueryStorer");
        }
        queries.add(q);
    }

    /**
     * Allows a Query to be added to this tracker.
     *
     * @param q a query String to be added to the tracker
     */
    @Override
    public void add(String q) {
        if (q == null) {
            throw new NullPointerException("Cannot add null queries to a BestQueryStorer");
        }
        queries.add(new Query(q));
    }

    /**
     * Gets the set of queries added to this object
     *
     * @return the set of queries
     */
    public Set getQueries() {
        return queries;
    }

    /**
     * Gets the best Query found so far
     *
     * @return the best Query, or null if no Queries added to this object
     */
    @Override
    public Query getBestQuery() {
        throw new RuntimeException("Unsupported Operation");
    }

    /**
     * Gets the best query String found so far
     *
     * @return the best Query, or null if no Queries added to this object
     */
    @Override
    public String getBestQueryString() {
        throw new RuntimeException("Unsupported Operation");
    }
}
