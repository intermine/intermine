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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.flymine.sql.Database;

/**
 * A class that provides an implementation of a cache for String-based SQL query optimisation.
 *
 * @author Matthew Wakeling
 */
public class OptimiserCache
{
    // Caches need to be per-database, so we will provide a static method to retrieve a cache object
    // given a database. We need to be careful about synchronisation in this whole class.
    private static Map caches = new HashMap();

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
        return ((OptimiserCache) caches.get(db));
    }


    /**
     * A Map that holds a mapping from unoptimised query string (with LIMIT and OFFSET stripped off)
     * to a Set of OptimiserCacheLine objects.
     */
    private Map cacheLines;
    
    /**
     * Private constructor for this object - should only be called by getInstance().
     */
    protected OptimiserCache() {
        cacheLines = new HashMap();
    }

    /**
     * Adds a new OptimiserCacheLine object to the cache.
     *
     * @param original the original SQL string (stripped of LIMIT and OFFSET)
     * @param optimised the optimised SQL string (stripped of LIMIT and OFFSET)
     * @param limit the limit that was used during the optimisation
     * @param offset the offset that was used during the optimisation
     * @param expectedRows the expected number of rows in the query results
     */
    public synchronized void addCacheLine(String original, String optimised, int limit,
            int offset, int expectedRows) {
        if (original.toUpperCase().startsWith("EXPLAIN ")) {
            original = original.substring(8);
        }
        if (optimised.toUpperCase().startsWith("EXPLAIN ")) {
            optimised = optimised.substring(8);
        }
        OptimiserCacheLine line = new OptimiserCacheLine(optimised, limit, offset, expectedRows);
        if (!cacheLines.containsKey(original)) {
            cacheLines.put(original, new HashSet());
        }

        Set lines = (Set) cacheLines.get(original);
        expire(lines);

        lines.add(line);
    }

    /**
     * Attempts to find a match in the cache for an original query.
     *
     * @param original the original SQL string (minus LIMIT and OFFSET)
     * @param limit the limit required
     * @param offset the offset required
     * @return a possible optimised SQL string (minus LIMIT and OFFSET)
     */
    public synchronized String lookup(String original, int limit, int offset) {
        //System//.out.print("Looking up query \"" + original + "\" with limit " + limit
        //        + " and offset " + offset + " - ");
        boolean originalWasExplain = false;
        if (original.toUpperCase().startsWith("EXPLAIN ")) {
            original = original.substring(8);
            originalWasExplain = true;
        }
        Set lines = (Set) cacheLines.get(original);
        if (lines == null) {
            // Couldn't find anything.
            //System//.out.println("Complete cache miss");
            return null;
        }
        double bestScore = Double.POSITIVE_INFINITY;
        OptimiserCacheLine bestLine = null;
        Iterator lineIter = lines.iterator();
        while (lineIter.hasNext()) {
            OptimiserCacheLine line = (OptimiserCacheLine) lineIter.next();
            if (line.isExpired()) {
                lineIter.remove();
            } else {
                double score = line.score(limit, offset);
                if (score < bestScore) {
                    bestScore = score;
                    bestLine = line;
                }
            }
        }
        if (bestScore > 1.0) {
            //System//.out.println("Cache didn't have anything near enough");
            return null;
        }
        //System//.out.println("Cache hit"); 
        return (originalWasExplain ? "EXPLAIN " : "") + bestLine.getOptimised();
    }

    /**
     * Removes expired entries from a Set of cache lines.
     *
     * @param lines a Set of OptimiserCacheLine objects
     */
    private static void expire(Set lines) {
        Iterator lineIter = lines.iterator();
        while (lineIter.hasNext()) {
            OptimiserCacheLine line = (OptimiserCacheLine) lineIter.next();
            if (line.isExpired()) {
                lineIter.remove();
            }
        }
    }
}
