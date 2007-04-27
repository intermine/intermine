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

/**
 * Logs each query added.
 *
 * @author Matthew Wakeling
 */
public class BestQueryLogger extends BestQuery
{
    protected boolean full;

    /**
     * Constructs a BestQueryLogger
     *
     * @param full true for a full log entry, false for a summary of each query
     */
    public BestQueryLogger(boolean full) {
        this.full = full;
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
        if (full) {
            System.out .println("Optimiser: " + q);
        } else {
            System.out .println("Optimiser: query with " + q.getFrom().size() + " FROM entries");
        }
    }

    /**
     * Allows a Query to be added to this tracker.
     *
     * @param q a query String to be added to the tracker
     */
    public void add(String q) {
        if (q == null) {
            throw new NullPointerException("Cannot add null queries to a BestQueryStorer");
        }
        System.out .println("Optimiser: " + q);
    }

    /**
     * Gets the best Query found so far
     *
     * @return the best Query, or null if no Queries added to this object
     */
    public Query getBestQuery() {
        throw new RuntimeException("Unsupported Operation");
    }

    /**
     * Gets the best query String found so far
     *
     * @return the best Query, or null if no Queries added to this object
     */
    public String getBestQueryString() {
        throw new RuntimeException("Unsupported Operation");
    }
}
