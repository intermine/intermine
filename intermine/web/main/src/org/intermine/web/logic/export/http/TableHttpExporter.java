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

import java.util.List;

import org.intermine.path.Path;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.struts.TableExportForm;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
     * @param form the form containing the columns paths to export
     */
    public void export(PagedTable pt, HttpServletRequest request,
                       HttpServletResponse response, TableExportForm form);

    /**
     * Check if this TableExporter can export the given PagedTable.
     * @param pt the PagedTable
     * @return true if and only if this TableExporter can export the argument PagedTable
     */
    public boolean canExport(PagedTable pt);

    /**
     * From the columns of the PagedTable, return a List of the Paths that this exporter will treat
     * specially.
     * eg. if the columns are ("Gene.primaryIdentifier", "Gene.secondaryIdentifier",
     * "Gene.proteins.primaryIdentifier") return ("Gene", "Gene.proteins").  This is needed for
     * exporters like SequenceExporter that act on certain classes only (for SequenceExporter,
     * classes that have a sequence reference)
     * @param pt the PagedTable
     * @return the list of possible Paths that can be exported or an empty List if this exporter can
     * only export all columns at once and doesn't treat some classes specially
     */
    public List<Path> getExportClassPaths(PagedTable pt);

    /**
     * Return a list of the Paths to show the user as initial export columns or header contents.
     * The List is likely to be based on the columns of the PagedTable plus or minus special cases
     * for this exporter.
     * @param pt the PagedTable
     * @return the Paths
     */
    public List<Path> getInitialExportPaths(PagedTable pt);
}
