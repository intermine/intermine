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

import org.intermine.web.logic.results.PagedTable;

/**
 * An implementation of QueryMonitor that maintains a timeout and remembers which PagedTable it is
 * monitoring
 * @author Kim Rutherford
 */
public class PageTableQueryMonitor extends QueryMonitorTimeout
{
    private final PagedTable pt;

    /**
     * Construct a new instance of QueryMonitorTimeout.
     *
     * @param n the number of milliseconds to timeout after
     * @param pt the PagedTable that we are monitoring
     */
    public PageTableQueryMonitor(int n, PagedTable pt) {
        super(n);
        this.pt = pt;
    }

    /**
     * Return the PagedTable that was passed to the constructor.
     * @return the PagedTable
     */
    public final PagedTable getPagedTable() {
        return pt;
    }


}
