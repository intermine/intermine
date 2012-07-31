package org.intermine.sql.precompute;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * A class that provides an implementation of a cache line, encapsulating several pieces of data.
 *
 * @author Matthew Wakeling
 */
public class OptimiserCacheLine
{
    private static final double MAX_LIMIT_FACTOR = 4.0; // a limit off by this factor will score 1.0

    private String optimised;
    private int limit;
    private String original;


    /**
     * Constructor for this object.
     *
     * @param optimised the optimised SQL String, minus the LIMIT and OFFSET
     * @param limit the limit that was used to generate optimised
     * @param original the original sql query
     */
    public OptimiserCacheLine(String optimised, int limit, String original) {
        this.optimised = optimised;
        this.limit = limit;
        this.original = original;
    }

    /**
     * Scores this cache line according to how far away the required limit is from the limit used to
     * create the line.
     *
     * @param limit the required limit
     * @return a double according to how close the match is. Less is better, with 1.0 as a
     * reasonable cut-off point to ignore the line
     */
    public double score(int limit) {
        double limitFactor = (limit > this.limit ? Math.log(limit) - Math.log(this.limit)
                : Math.log(this.limit) - Math.log(limit));
        return (limitFactor / Math.log(MAX_LIMIT_FACTOR));
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
     * Returns the original SQL string.
     *
     * @return the original SQL string
     */
    public String getOriginal() {
        return original;
    }
}

