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

/**
 * A class describing settings for the optimiser. One should create an instance of this class,
 * alter the settings to suit, and pass it in to the optimise methods.
 *
 * @author Matthew Wakeling
 */
public class QueryOptimiserContext
{
    /** Normal operation - no logging */
    public static final String MODE_NORMAL = "MODE_NORMAL";
    /** Normal operation, plus logging to stdout */
    public static final String MODE_VERBOSE = "MODE_VERBOSE";
    /** Logs all generated queries to the stdout, without explaining */
    public static final String MODE_VERBOSE_LIST = "MODE_VERBOSE_LIST";
    /** Summarises all generated queries to the stdout, without explaining */
    public static final String MODE_VERBOSE_SUMMARY = "MODE_VERBOSE_SUMMARY";

    private String mode = MODE_NORMAL;
    private long timeLimit = -1;
    // This is the maximum amout of time that should be spent parsing an SQL query from a string
    // to a Query object in milliseconds. It can be overwritten by the property:
    // os.query.max-query-parse-time=200
    private long maxQueryParseTime = 100;


    /**
     * Sets the optimiser mode of operation.
     *
     * @param mode MODE_NORMAL, MODE_VERBOSE, MODE_VERBOSE_LIST, or MODE_VERBOSE_SUMMARY
     */
    public void setMode(String mode) {
        if ((mode != MODE_NORMAL) && (mode != MODE_VERBOSE) && (mode != MODE_VERBOSE_LIST)
                && (mode != MODE_VERBOSE_SUMMARY)) {
            throw new IllegalArgumentException("Invalid mode " + mode);
        }
        this.mode = mode;
    }

    /**
     * Returns the optimiser mode of operation.
     *
     * @return mode
     */
    public String getMode() {
        return mode;
    }

    /**
     * Sets the optimiser's maximum effort time, in milliseconds. The optimiser will cease
     * optimising the query if it notices that it has spent longer than this number of
     * milliseconds. Note that this value is not a deadline - it will not be kept. The optimiser
     * can take as much as the time taken to explain a query more than this value. A value of -1
     * indicates that there is no limit. A value of zero will result in only one query being
     * explained - this may or may not be the original query.
     *
     * @param timeLimit time in milliseconds
     */
    public void setTimeLimit(long timeLimit) {
        if (timeLimit < -1) {
            throw new IllegalArgumentException("Invalid time limit " + timeLimit);
        }
        this.timeLimit = timeLimit;
    }

    /**
     * Returns the time limit.
     *
     * @return time limit
     */
    public long getTimeLimit() {
        return timeLimit;
    }



    /**
     * Returns true if the optimiser will print out stuff.
     *
     * @return a boolean
     */
    public boolean isVerbose() {
        return (MODE_VERBOSE == mode) || (MODE_VERBOSE_LIST == mode)
            || (MODE_VERBOSE_SUMMARY == mode);
    }

    /**
     * Get the maximum time in milliseconds that the optimiser should spend parsing a query from
     * SQL string to an org.intermine.sql.Query object.
     * @return the max query parse time
     */
    public long getMaxQueryParseTime() {
        return maxQueryParseTime;
    }

    /**
     * Set the maximum time in milliseconds that the optimiser should spend parsing a query from
     * SQL string to an org.intermine.sql.Query object.
     * @param maxQueryParseTime the max query parse time
     */
    public void setMaxQueryParseTime(long maxQueryParseTime) {
        this.maxQueryParseTime = maxQueryParseTime;
    }

    /**
     * The default context - normal operation with no time limit.
     */
    public static final QueryOptimiserContext DEFAULT = new QueryOptimiserContext() {
        @Override
        public void setMode(@SuppressWarnings("unused") String mode) {
            throw new IllegalStateException("This is the default QueryOptimiserContext - it cannot"
                    + " be altered");
        }

        @Override
        public String getMode() {
            return MODE_NORMAL;
        }

        @Override
        public void setTimeLimit(@SuppressWarnings("unused") long timeLimit) {
            throw new IllegalStateException("This is the default QueryOptimiserContext - it cannot"
                    + " be altered");
        }

        @Override
        public long getTimeLimit() {
            return -1;
        }
    };
}
