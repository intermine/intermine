package org.intermine.web.logic.export.http;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.web.logic.results.PagedTable;

/**
 * Implemented by objects that can export PagedTable. 
 *
 * @author Kim Rutherford
 */

public interface TableHttpExporter
{
    /**
     * Method called to export a PagedTable object
     * @param pt exported PagedTable
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     */
    public void export(PagedTable pt, HttpServletRequest request,
                                HttpServletResponse response);

    /**
     * Check if this TableExporter can export the given PagedTable.
     * @param pt the PagedTable
     * @return true if and only if this TableExporter can export the argument PagedTable
     */
    public boolean canExport(PagedTable pt);
}
