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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.intermine.web.logic.results.PagedTable;

/**
 * Implemented by objects that can export PagedTables.
 *
 * @author Kim Rutherford
 */

public interface TableExporter
{
    /** 
     * Method called to export a PagedTable object
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward export(ActionMapping mapping,
                                ActionForm form,
                                HttpServletRequest request,
                                HttpServletResponse response)
        throws Exception;

    /**
     * Check if this TableExporter can export the given PagedTable.
     * @param pt the PagedTable
     * @return true if and only if this TableExporter can export the argument PagedTable
     */
    public boolean canExport(PagedTable pt);
}
