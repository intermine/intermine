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

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.web.logic.export.ExportException;
import org.intermine.web.logic.export.Exporter;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.results.ResultElement;
import org.intermine.web.logic.session.SessionMethods;


/**
 * Abstract class that implements basic functionality common for exporters
 * exporting table with results in simple format like comma separated format.
 * The business logic of export is performed with exporter obtained via 
 * getExport() method and so each subclass can redefine it overwriting this method.
 * @author Jakub Kulaviak
 **/
public abstract class HttpExporterBase implements TableHttpExporter
{

    /**
     * Constructor.
     */
    public HttpExporterBase() { }
        
    /**
     * @param pt PagedTable
     * @return true if given PagedTable can be exported with this exporter
     */
    public boolean canExport(PagedTable pt) {
        return true;
    }

    /**
     * Perform export.
     * @param mapping Struts action mapping 
     * @param form Struts action form
     * @param request request
     * @param response response
     * @throws Exception if some error happens
     * @return forward
     */
    public ActionForward export(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        HttpSession session = request.getSession();
        PagedTable pt = getPagedTable(request, session);
        List<List<ResultElement>> results = pt.getRearrangedResults();
        if (pt == null) {
            // TODO display error?
            //return mapping.getInputForward();
        }        
        
        OutputStream out = null;
        try {
            out = response.getOutputStream();
        } catch (IOException e) {
            throw new ExportException("Export failed.", e);
        }
        setResponseHeader(response);
        getExporter(out).export(results);
        return null;
    }

    /**
     * @param out output stream
     * @return exporter that will perform the business logic of export. 
     */
    protected abstract Exporter getExporter(OutputStream out);
    
    /**
     * Sets header and content type of result in response.
     * @param response response
     */
    protected abstract void setResponseHeader(HttpServletResponse response);
    
    private PagedTable getPagedTable(HttpServletRequest request, HttpSession session) {
        PagedTable pt;
        String tableType = request.getParameter("tableType");
        if (tableType.equals("bag")) {
            pt = SessionMethods.getResultsTable(session, "bag." 
                    + request.getParameter("table"));
        } else {
            pt = SessionMethods.getResultsTable(session, request.getParameter("table"));
        }
        return pt;
    }
}
