package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;

import org.intermine.objectstore.query.Results;

/**
 * Interface passed to SessionMethods.runQuery which is polled every so-often while the
 * query runs so that some other task can be carried out. Used to provide visual feedback
 * to the user while a query is running and to detect when the client's browser 'hangs up'
 * on us.
 *
 * @author Tom Riley
 */
public interface RunQueryMonitor
{
    /**
     * Called intermittently while a query is run.
     *
     * @param results the Results object associated with the running query
     * @param request the http servlet request
     * @return false if the query should be cancelled, otherwise true
     */
    public boolean queryProgress(HttpServletRequest request, Results results);
}
