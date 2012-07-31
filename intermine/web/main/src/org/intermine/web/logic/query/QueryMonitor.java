package org.intermine.web.logic.query;

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
 * Interface passed to SessionMethods runQuery method which is polled every so-often while the
 * query runs in order to decide whether or not to cancel the query. Instances of this
 * interface are used in conjunction with a periodic browser refresh to detect and act upon
 * user cancellation (the user closing their browser window or navigating to a different page).
 *
 * @author Thomas Riley
 */
public interface QueryMonitor
{
    /**
     * Called intermittently while a query is run providing an opportunity to cancel
     * the query.
     *
     * param results the Results object associated with the running query
     * param request the http servlet request
     * @return false if the query should be cancelled, otherwise true
     */
    boolean shouldCancelQuery();

    /**
     * Called when the query has completed.
     */
    void queryCompleted();

    /**
     * Called when the query stopped with an error. The error message should be
     * in the session.
     */
    void queryCancelledWithError();

    /**
     * Called when the query is cancelled. Usually as a result of shouldCancelQuery
     * returning true.
     */
    void queryCancelled();
}
