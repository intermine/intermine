package org.intermine.web.logic.export.http;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.struts.TableExportForm;

/**
 * Implemented by objects that can export PagedTable.
 *
 * @author Kim Rutherford
 */

@SuppressWarnings("deprecation")
public interface TableHttpExporter
{
    /**
     * Method called to export a PagedTable object
     * @param pt exported PagedTable
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @param form the form containing the columns paths to export
     * @param unionPathCollection a collection of Path combining old and new views from pathquery
     * @param newPathCollection a collection of Path, from user columns paths
     */
    void export(PagedTable pt, HttpServletRequest request,
            HttpServletResponse response, TableExportForm form,
            Collection<Path> unionPathCollection, Collection<Path> newPathCollection);

    /**
     * Check if this TableExporter can export the given PagedTable.
     * @param pt the PagedTable
     * @return true if and only if this TableExporter can export the argument PagedTable
     */
    boolean canExport(PagedTable pt);

    /**
     * Return a list of the Paths to show the user as initial export columns or header contents.
     * The List is likely to be based on the columns of the PagedTable plus or minus special cases
     * for this exporter.
     * @param pt the PagedTable
     * @return the Paths
     * @throws PathException if bad path encountered
     */
    List<Path> getInitialExportPaths(PagedTable pt) throws PathException;
}
