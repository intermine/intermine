package org.flymine.sql.precompute;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.flymine.sql.query.Query;
import java.util.Set;
import java.sql.SQLException;
import org.flymine.util.ConsistentSet;

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
     * @throws BestQueryException if the current best Query is the best we think we are going to get
     * @throws SQLException if there is an error in the underlying database
     */
    public void add(Query q) throws BestQueryException, SQLException {
        if (q == null) {
            throw new NullPointerException("Cannot add null queries to a BestQueryStorer");
        }
        queries.add(q);
    }

    /**
     * Allows a Query to be added to this tracker.
     *
     * @param q a query String to be added to the tracker
     * @throws BestQueryException if the current best Query is the best we think we are going to get
     * @throws SQLException if there is an error in the underlying database
     */
    public void add(String q) throws BestQueryException, SQLException {
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
}
