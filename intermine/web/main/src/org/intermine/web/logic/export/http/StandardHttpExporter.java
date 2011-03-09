package org.intermine.web.logic.export.http;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.results.ExportResultsIterator;
import org.intermine.pathquery.Path;
import org.intermine.web.logic.RequestUtil;
import org.intermine.web.logic.export.ExportException;
import org.intermine.web.logic.export.ExportHelper;
import org.intermine.web.logic.export.Exporter;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.struts.TableExportForm;

/**
 * Abstract class implementing functionality common for exporters exporting table in simple format
 * like comma separated format. The business logic of export is performed with exporter obtained
 * via getExport method and so each subclass can redefine it overwriting this method.
 *
 * @author Jakub Kulaviak
 **/
public abstract class StandardHttpExporter extends HttpExporterBase implements TableHttpExporter
{

    /**
     * Constructor.
     */
    public StandardHttpExporter() { }

    /**
     * @param pt PagedTable
     * @return true if given PagedTable can be exported with this exporter
     */
    public boolean canExport(@SuppressWarnings("unused") PagedTable pt) {
        return true;
    }

    /**
     * Perform export.
     * @param pt exported PagedTable
     * @param request request
     * @param response response
     * @param form the form
     */
    public void export(PagedTable pt, HttpServletRequest request,
                       HttpServletResponse response, TableExportForm form) {
        boolean doGzip = (form != null) && form.getDoGzip();
        OutputStream out = null;
        try {
            out = response.getOutputStream();
            if (doGzip) {
                out = new GZIPOutputStream(out);
            }
        } catch (IOException e) {
            throw new ExportException("Export failed.", e);
        }
        setResponseHeader(response, doGzip);
        String separator;
        if (RequestUtil.isWindowsClient(request)) {
            separator = Exporter.WINDOWS_SEPARATOR;
        } else {
            separator = Exporter.UNIX_SEPARATOR;
        }
        List<String> headers = null;
        if (form != null && form.getIncludeHeaders()) {
            headers = getHeaders(pt);
        }
        Exporter exporter = getExporter(out, separator, headers);
        ExportResultsIterator iter = null;
        try {
            iter = getResultRows(pt, request);
            iter.goFaster();
            exporter.export(iter);
            if (out instanceof GZIPOutputStream) {
                try {
                    ((GZIPOutputStream) out).finish();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } finally {
            if (iter != null) {
                iter.releaseGoFaster();
            }
        }
        if (exporter.getWrittenResultsCount() == 0) {
            throw new ExportException("Nothing was found for export.");
        }
    }

    private List<String> getHeaders(PagedTable pt) {
        List<String> headers;
        headers = new ArrayList<String>();
        for (String columnName: pt.getColumnNames()) {
            headers.add(columnName);
        }
        return headers;
    }

    /**
     * The initial export path list is just the paths from the columns of the PagedTable.
     * {@inheritDoc}
     */
    public List<Path> getInitialExportPaths(PagedTable pt) {
        return ExportHelper.getColumnPaths(pt);
    }

    /**
     * Do the export.
     * @param out output stream
     * @param separator line separator
     * @param headers if non-null, a list of the column headers which will be written by export()
     * @return exporter that will perform the business logic of export.
     */
    protected abstract Exporter getExporter(OutputStream out, String separator,
                                            List<String> headers);

    /**
     * Sets header and content type of result in response.
     *
     * @param response response
     * @param doGzip whether to compress the stream
     */
    protected abstract void setResponseHeader(HttpServletResponse response, boolean doGzip);
}
