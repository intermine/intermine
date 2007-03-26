package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Represents a result from the estimate() method of an ObjectStore.
 *
 * @author Matthew Wakeling
 */
public class ResultsInfo
{
    protected long start;
    protected long complete;
    protected int min;
    protected int rows;
    protected int max;

    /** Denotes that the result of getRows() is the known size of the results. */
    public static final int SIZE = 0;
    /** Denotes that the known number of rows has a maximum, and the estimate exceeds it. */
    public static final int AT_MOST = 1;
    /** Denotes that the known number of rows has a minimum which exceed the estimate. */
    public static final int AT_LEAST = 2;
    /** Denotes that the result of getRows() is a pure estimate that is not contradicted by the min
     * or max */
    public static final int ESTIMATE = 3;
    
    /**
     * No-arg constructor to allow serialization
     */
    public ResultsInfo() {
    }
    
    /**
     * Constructs an instance of ResultsInfo with all new parameters, without min and max.
     *
     * @param start the estimated amount of time required to fetch the first row of results, in
     * milliseconds
     * @param complete the estimated amount of time required to fetch the full set of results, in
     * milliseconds
     * @param rows the best current estimate of the number of rows that the results contains,
     * neglecting information available about the minimum possible and maximum possible number
     */
    public ResultsInfo(long start, long complete, int rows) {
        this(start, complete, rows, 0, Integer.MAX_VALUE);
    }

    /**
     * Constructs an instance of ResultsInfo with all new parameters.
     *
     * @param start the estimated amount of time required to fetch the first row of results, in
     * milliseconds
     * @param complete the estimated amount of time required to fetch the full set of results, in
     * milliseconds
     * @param rows the best current estimate of the number of rows that the results contains,
     * neglecting information available about the minimum possible and maximum possible number
     * @param min the minimum possible number of rows
     * @param max the maximum possible number of rows
     */
    public ResultsInfo(long start, long complete, int rows, int min, int max) {
        this.start = start;
        this.complete = complete;
        this.rows = rows;
        this.min = min;
        this.max = max;
    }

    /**
     * Constructs an instance of ResultsInfo from another ResultsInfo and a new min and max.
     *
     * @param old the old ResultsInfo object from which to copy data
     * @param min the minimum possible number of rows
     * @param max the maximum possible number of rows
     */
    public ResultsInfo(ResultsInfo old, int min, int max) {
        this.start = old.start;
        this.complete = old.complete;
        this.rows = old.rows;
        this.min = (old.min > min ? old.min : min);
        this.max = (old.max > max ? max : old.max);
    }

    /**
     * Returns an integer describing the type of estimate getRows() returns.
     *
     * @return SIZE, ESTIMATE, AT_MOST, AT_LEAST
     */
    public int getStatus() {
        if (min == max) {
            return SIZE;
        } else if (min >= rows) {
            return AT_LEAST;
        } else if (max <= rows) {
            return AT_MOST;
        }
        return ESTIMATE;
    }

    /**
     * Returns the estimated number of rows in the results.
     *
     * @return an int
     */
    public int getRows() {
        return (rows > min ? (rows < max ? rows : max) : min);
    }

    /**
     * Sets the estimated number of rows in the results.
     * NOTE: this method is only present to make this class a Bean.
     *
     * @param rows the new rows variable
     */
    public void setRows(int rows) {
        this.rows = rows;
    }
    
    /**
     * Returns the estimated amount of time taken to produce the first row of the results.
     *
     * @return a long of milliseconds
     */
    public long getStart() {
        return start;
    }

    /**
     * Sets the estimated amount of time taken to produce the first row of the results.
     * NOTE: this method is only present to make this class a Bean.
     *
     * @param start a long
     */
    public void setStart(long start) {
        this.start = start;
    }
    
    /**
     * Returns the estimated amount of time taken to produce the entire set of results.
     *
     * @return a long of milliseconds
     */
    public long getComplete() {
        return complete;
    }

    /**
     * Sets the estimated amount of time taken to produce the entire set of results.
     * NOTE: this method is only present to make this class a Bean.
     *
     * @param complete a long
     */
    public void setComplete(long complete) {
        this.complete = complete;
    }
    
    /**
     * Returns the minimum possible number of rows in the results.
     *
     * @return an int
     */
    public int getMin() {
        return min;
    }

    /**
     * Sets the minimum possible number of rows in the results.
     * NOTE: this method is only present to make this class a Bean.
     *
     * @param min an int
     */
    public void setMin(int min) {
        this.min = min;
    }
    
    /**
     * Returns the maximum possible number of rows in the results.
     *
     * @return an int
     */
    public int getMax() {
        return max;
    }

    /**
     * Sets the maximum possible number of rows in the results.
     * NOTE: this method is only present to make this class a Bean.
     *
     * @param max an int
     */
    public void setMax(int max) {
        this.max = max;
    }

    /**
     * @see Object#equals
     */
    public boolean equals(Object o) {
        if (o instanceof ResultsInfo) {
            ResultsInfo i = (ResultsInfo) o;
            return i.start == start
                && i.complete == complete
                && i.rows == rows
                && i.min == min
                && i.max == max;
        }
        return false;
    }

    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return  (int) (2 * start
            +  3 * complete
            + 5 * rows
            + 7 * min
            + 9 * max);
    }

    /**
     * @see Object#toString
     */
    public String toString() {
        return "ResultsInfo(start=" + start + ", complete=" + complete + ", rows=" + rows
            + ", min=" + min + ", max=" + max + ")";
    }
}
