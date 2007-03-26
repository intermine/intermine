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

import java.util.Date;
import java.util.Set;

/**
 * A class that provides an implementation of a cache line, encapsulating several pieces of data.
 * 
 * @author Matthew Wakeling
 */
public class OptimiserCacheLine
{
    private static final long CACHE_LINE_LIFETIME = 1200000; // 20 minutes.
    private static final double MAX_LIMIT_FACTOR = 4.0; // a limit off by this factor will score 1.0
    private static final double OFFSET_SECTIONS = 8.0; // while paging through a large query with a
                                                       // constant LIMIT, optimise this many times.
    private static final int OFFSET_GRACE = 1000; // The difference in offset may be this large
                                                  // before any score is produced.
    
    private String optimised;
    private int limit;
    private int offset;
    private int expectedRows;
    protected Date expires;
    private Set lineSet;
    private String original;
    

    /**
     * Constructor for this object.
     *
     * @param optimised the optimised SQL String, minus the LIMIT and OFFSET
     * @param limit the limit that was used to generate optimised
     * @param offset the offset that was used to generate optimised
     * @param expectedRows the expected number of rows in the query results
     * @param lineSet the lineSet parent of this line
     * @param original the original sql query
     */
    public OptimiserCacheLine(String optimised, int limit, int offset, int expectedRows,
            Set lineSet, String original) {
        this.optimised = optimised;
        this.limit = limit;
        this.offset = offset;
        this.expectedRows = expectedRows;
        this.lineSet = lineSet;
        this.original = original;
        expires = new Date((new Date()).getTime() + CACHE_LINE_LIFETIME);
    }

    /**
     * Returns true if this object has expired.
     *
     * @return true if this object has expired.
     */
    public boolean isExpired() {
        return (new Date()).after(expires);
    }

    /**
     * Scores this cache line according to how far away the required limit and offset are from the
     * limit and offset used to create the line.
     *
     * @param limit the required limit
     * @param offset the required offset
     * @return a double according to how close the match is. Less is better, with 1.0 as a
     * reasonable cut-off point to ignore the line
     */
    public double score(int limit, int offset) {
        int offsetDiff = (offset > this.offset ? offset - this.offset : this.offset - offset);
        offsetDiff = (offsetDiff > OFFSET_GRACE ? offsetDiff - OFFSET_GRACE : 0);
        double limitFactor = (limit > this.limit ? Math.log(limit) - Math.log(this.limit)
                : Math.log(this.limit) - Math.log(limit));
        return (limitFactor / Math.log(MAX_LIMIT_FACTOR))
            + ((offsetDiff * OFFSET_SECTIONS) / (expectedRows * 1.0));
    }

    /**
     * Gets the optimised query (minus the LIMIT and OFFSET) from this cache line.
     *
     * @return the optimised query
     */
    public String getOptimised() {
        return optimised;
    }

    /**
     * Returns the expiry date of this cache line.
     *
     * @return the expiry date
     */
    public Date getExpiry() {
        return expires;
    }

    /**
     * Returns the lineSet that this line is a member of.
     *
     * @return the lineSet
     */
    public Set getLineSet() {
        return lineSet;
    }

    /**
     * Returns the original SQL string.
     *
     * @return the original SQL string
     */
    public String getOriginal() {
        return original;
    }
}

