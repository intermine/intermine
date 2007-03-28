package org.intermine.web.logic;

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
 * QueryMonitor that cancels a query after a given timeout period. An instance of this method
 * should be created, passed to SessionMethods.runQuery and then services repeatedly by calling
 * the tickle method periodically. The timeout period should be significantly longer than the
 * tickle period (so a little variance in the time between tickle calls won't cause an
 * unwanted timeout). Note that the constructor calls tickle to initialise the timing so you
 * should create this object when you need it (and will immediately tickle it) and not before.<p>
 * 
 * The shouldCancelQuery method will return true if the tickle method has not been called in the
 * past n milliseconds where n is set during construction.
 *
 * @author Thomas Riley
 */
public class QueryMonitorTimeout implements QueryMonitor
{
    /** Number of milliseconds before timeout. */
    protected int n;
    /** Time of last call to tickle. */
    protected long lastTickle = -1;
    /** number of tickles. used to determine how. */
    protected int tickleCount = 0;
    /** Set to true when query completes. */
    private boolean complete = false;
    /** Set to true on a call to queryCancelledWithError. */
    private boolean error = false;
    /** Set to true on a call to queryCancelled. */
    private boolean cancelled = false;
    
    /**
     * Construct a new instance of QueryMonitorTimeout.
     *
     * @param n the number of milliseconds to timeout after
     */
    public QueryMonitorTimeout(int n) {
        this.n = n;
        tickle();
    }
    
    /**
     * Return true if tickle has not been called in a period of time greater than the
     * timeout value.
     *
     * @see QueryMonitor#shouldCancelQuery()
     */
    public boolean shouldCancelQuery() {
        return ((System.currentTimeMillis() - lastTickle) > n);
    }
    
    /**
     * Tell this object to continue returning false from shouldCancelQuery. If this method is
     * not called for a period of time greater than the timeout period, shouldCancelQuery will
     * return true.
     */
    public void tickle() {
        lastTickle = System.currentTimeMillis();
        tickleCount++;
    }
    
    /**
     * Find out how many times tickle has been called.
     * 
     * @return number of times tickle has been called
     */
    public int getTickleCount() {
        return tickleCount;
    }
    
    /**
     * @see QueryMonitor#queryCompleted()
     */
    public void queryCompleted() {
        complete = true;
    }
    
    /**
     * Find out whether the query has completed.
     * 
     * @return true when the query has completed
     */
    public boolean isCompleted() {
        return complete;
    }

    /**
     * @see QueryMonitor#queryCancelledWithError
     */
    public void queryCancelledWithError() {
        error = true;
    }

    /**
     * Find out whether an error occured while trying to run the query.
     * 
     * @return true if an error occured, false if not
     */
    public boolean isCancelledWithError() {
        return error;
    }
    
    /**
     * Find out whether the query was cancelled.
     * 
     * @return true if query was cancelled, false if not
     */
    public boolean isCancelled() {
        return cancelled;
    }
    
    /**
     * @see QueryMonitor#queryCancelled
     */
    public void queryCancelled() {
        cancelled = true;
    }
}
