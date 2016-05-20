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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.intermine.sql.Database;
import org.intermine.util.CacheMap;

/**
 * A class that provides an implementation of a cache for String-based SQL query optimisation.
 *
 * @author Matthew Wakeling
 */
public class OptimiserCache
{
    /** Maximum number of cache linesets in the cache. */
    public static final int MAX_LINESETS = 1000;
    /** Number of events to happen before an expiration run. */
    public static final int EXPIRE_INTERVAL = 100;

    // Caches need to be per-database, so we will provide a static method to retrieve a cache object
    // given a database. We need to be careful about synchronisation in this whole class.
    private static Map<Database, OptimiserCache> caches = new HashMap<Database, OptimiserCache>();

    /**
     * Returns an OptimiserCache object relevant to the database given.
     *
     * @param db a Database object to find a cache for
     * @return an OptimiserCache object
     */
    public static synchronized OptimiserCache getInstance(Database db) {
        if (!caches.containsKey(db)) {
            caches.put(db, new OptimiserCache());
        }
        return caches.get(db);
    }


    /**
     * A Map that holds a mapping from unoptimised query string (with LIMIT and OFFSET stripped off)
     * to a Set of OptimiserCacheLine objects.
     */
    protected Map<String, Set<OptimiserCacheLine>> cacheLines;

    /**
     * Constructor for this object.
     */
    public OptimiserCache() {
        cacheLines = new CacheMap<String, Set<OptimiserCacheLine>>();
    }

    /**
     * Removes all entries from the cache.
     */
    public synchronized void flush() {
        cacheLines.clear();
    }

    /**
     * Adds a new OptimiserCacheLine object to the cache.
     *
     * @param original the original SQL string (stripped of LIMIT and OFFSET)
     * @param optimised the optimised SQL string (stripped of LIMIT and OFFSET)
     * @param limit the limit that was used during the optimisation
     */
    public synchronized void addCacheLine(String original, String optimised, int limit) {
        if (original.toUpperCase().startsWith("EXPLAIN ")) {
            original = original.substring(8);
        }
        if (optimised.toUpperCase().startsWith("EXPLAIN ")) {
            optimised = optimised.substring(8);
        }
        Set<OptimiserCacheLine> lines = cacheLines.get(original);
        if (lines == null) {
            lines = new HashSet<OptimiserCacheLine>();
            cacheLines.put(original, lines);
        }

        OptimiserCacheLine line = new OptimiserCacheLine(optimised, limit, original);

        lines.add(line);
    }

    /**
     * Attempts to find a match in the cache for an original query.
     *
     * @param original the original SQL string (minus LIMIT and OFFSET)
     * @param limit the limit required
     * @return a possible optimised SQL string (minus LIMIT and OFFSET)
     */
    public synchronized String lookup(String original, int limit) {
        //LOG.debug("Looking up query \"" + original + "\" with limit " + limit
        //        + " and offset " + offset + " - ");
        boolean originalWasExplain = false;
        if (original.toUpperCase().startsWith("EXPLAIN ")) {
            original = original.substring(8);
            originalWasExplain = true;
        }
        Set<OptimiserCacheLine> lines = cacheLines.get(original);
        if (lines == null) {
            // Couldn't find anything.
            //LOG.debug("Complete cache miss");
            return null;
        }
        double bestScore = Double.POSITIVE_INFINITY;
        OptimiserCacheLine bestLine = null;
        for (OptimiserCacheLine line : lines) {
            double score = line.score(limit);
            if (score < bestScore) {
                bestScore = score;
                bestLine = line;
            }
        }
        if (bestScore > 1.0) {
            //LOG.debug("Cache didn't have anything near enough");
            return null;
        }
        //LOG.debug("Cache hit");
        return (originalWasExplain ? "EXPLAIN " : "") + bestLine.getOptimised();
    }
}
