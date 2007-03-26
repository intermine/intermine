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

import org.apache.log4j.Logger;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.intermine.sql.Database;

/**
 * A class that provides an implementation of a cache for String-based SQL query optimisation.
 *
 * @author Matthew Wakeling
 */
public class OptimiserCache
{
    private static final Logger LOG = Logger.getLogger(OptimiserCache.class);

    /** Maximum number of cache linesets in the cache. */
    public static final int MAX_LINESETS = 1000;
    /** Number of events to happen before an expiration run. */
    public static final int EXPIRE_INTERVAL = 100;

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
    protected Map cacheLines;
    protected TreeMap evictionQueue;
    protected int sequence = 0;
    protected int untilNextExpiration = EXPIRE_INTERVAL;
    
    /**
     * Constructor for this object.
     */
    public OptimiserCache() {
        cacheLines = new HashMap();
        evictionQueue = new TreeMap();
    }

    /**
     * Removes all entries from the cache.
     */
    public synchronized void flush() {
        cacheLines.clear();
        evictionQueue.clear();
        untilNextExpiration = EXPIRE_INTERVAL;
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
        expire();
        if (original.toUpperCase().startsWith("EXPLAIN ")) {
            original = original.substring(8);
        }
        if (optimised.toUpperCase().startsWith("EXPLAIN ")) {
            optimised = optimised.substring(8);
        }
        if (!cacheLines.containsKey(original)) {
            cacheLines.put(original, new HashSet());
        }

        Set lines = (Set) cacheLines.get(original);
        OptimiserCacheLine line = new OptimiserCacheLine(optimised, limit, offset, expectedRows,
                lines, original);
        DateAndSequence d = new DateAndSequence(line.getExpiry(), sequence++);

        lines.add(line);
        evictionQueue.put(d, line);
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
        expire();
        //LOG.debug("Looking up query \"" + original + "\" with limit " + limit
        //        + " and offset " + offset + " - ");
        boolean originalWasExplain = false;
        if (original.toUpperCase().startsWith("EXPLAIN ")) {
            original = original.substring(8);
            originalWasExplain = true;
        }
        Set lines = (Set) cacheLines.get(original);
        if (lines == null) {
            // Couldn't find anything.
            //LOG.debug("Complete cache miss");
            return null;
        }
        double bestScore = Double.POSITIVE_INFINITY;
        OptimiserCacheLine bestLine = null;
        Iterator lineIter = lines.iterator();
        while (lineIter.hasNext()) {
            OptimiserCacheLine line = (OptimiserCacheLine) lineIter.next();
            double score = line.score(limit, offset);
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

    /**
     * Removes expired entries from the OptimiserCache, by looking at the evictionQueue.
     */
    protected void expire() {
        if ((--untilNextExpiration) <= 0) {
            untilNextExpiration = EXPIRE_INTERVAL;
            while (cacheLines.size() > MAX_LINESETS) {
                //LOG.debug("cacheLines.size = " + cacheLines.size() + ", evictionQueue.size = "
                //        + evictionQueue.size());
                DateAndSequence d = (DateAndSequence) evictionQueue.firstKey();
                OptimiserCacheLine line = (OptimiserCacheLine) evictionQueue.remove(d);
                expire(line);
            }
            Date now = new Date();
            DateAndSequence nextD = (evictionQueue.isEmpty() ? null
                    : (DateAndSequence) evictionQueue.firstKey());
            while ((nextD != null) && nextD.getDate().before(now)) {
                OptimiserCacheLine line = (OptimiserCacheLine) evictionQueue.remove(nextD);
                expire(line);
                nextD = (evictionQueue.isEmpty() ? null
                        : (DateAndSequence) evictionQueue.firstKey());
            }
        }
    }

    /**
     * Removes a particular cache line.
     * 
     * @param line the cache line to remove
     */
    private void expire(OptimiserCacheLine line) {
        if (line != null) {
            Set lines = line.getLineSet();
            String original = line.getOriginal();
            lines.remove(line);
            //LOG.debug("Expired cache line for original query " + original);
            if (lines.isEmpty()) {
                cacheLines.remove(original);
                //LOG.debug("Expired entire cache lineset for original query " + original);
            }
        } else {
            LOG.error("Expire called on null OptimiserCacheLine");
        }
    }

    /**
     * Class representing a date, but with the added advantage that no two of these should compare
     * equals if one is careful with the sequence.
     */
    protected static class DateAndSequence implements Comparable
    {
        private Date date;
        private int sequence;

        /**
         * Create a new instance.
         *
         * @param date a Date
         * @param sequence an integer
         */
        public DateAndSequence(Date date, int sequence) {
            this.date = date;
            this.sequence = sequence;
        }

        /**
         * @see Comparable#compareTo
         */
        public int compareTo(Object o) {
            if (o instanceof DateAndSequence) {
                DateAndSequence d = (DateAndSequence) o;
                int retval = date.compareTo(d.date);
                if (retval == 0) {
                    retval = sequence - d.sequence;
                }
                return retval;
            }
            throw new ClassCastException("Object is not an OptimiserCache.DateAndSequence");
        }

        /**
         * Getter for date.
         *
         * @return date
         */
        public Date getDate() {
            return date;
        }
    }
}
