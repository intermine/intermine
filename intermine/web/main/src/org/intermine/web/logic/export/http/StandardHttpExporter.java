package org.intermine.web.logic.export.http;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.intermine.api.results.Column;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.pathquery.Path;
import org.intermine.web.logic.RequestUtil;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.ExportException;
import org.intermine.web.logic.export.ExportHelper;
import org.intermine.web.logic.export.Exporter;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;
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
    protected static final Logger LOG = Logger.getLogger(StandardHttpExporter.class);

    /**
     * Constructor.
     */
    public StandardHttpExporter() { }

    /**
     * @param pt PagedTable
     * @return true if given PagedTable can be exported with this exporter
     */
    @Override
    public boolean canExport(final PagedTable pt) {
        return true;
    }

    /**
     * Perform export.
     * @param pt exported PagedTable
     * @param request request
     * @param response response
     * @param form the form
     * @param unionPathCollection view paths
     * @param newPathCollection columns paths
     */
    @Override
    public void export(final PagedTable pt, final HttpServletRequest request,
            final HttpServletResponse response, final TableExportForm form,
            final Collection<Path> unionPathCollection, final Collection<Path> newPathCollection) {

        final boolean doGzip = form != null && form.getDoGzip();
        OutputStream out = null;
        try {
            out = response.getOutputStream();
            if (doGzip) {
                out = new GZIPOutputStream(out);
            }
        } catch (final IOException e) {
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
            headers = getHeaders(pt, SessionMethods.getWebConfig(request), newPathCollection);
        }
        final Exporter exporter = getExporter(out, separator, headers);
        ExportResultsIterator iter = null;
        try {
            iter = getResultRows(pt, request);
            iter.goFaster();
            exporter.export(iter, unionPathCollection, newPathCollection);
            if (out instanceof GZIPOutputStream) {
                try {
                    ((GZIPOutputStream) out).finish();
                } catch (final IOException e) {
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

    /**
     * Get headers for the export table. If there is a pathquery, try and use its path descriptions,
     * otherwise format the columns we do have with the labels configured in the web applications
     * configurations.
     * @param pt The paged table we are trying to export
     * @param webConfig The web application's display configuration.
     * @return A list of headers.
     */
    private List<String> getHeaders(final PagedTable pt, final WebConfig webConfig,
            final Collection<Path> pathCollection) {

        final List<String> headers = new ArrayList<String>();

        List<Column> ptCols = pt.getColumns();
        List<Path> colPathList = new ArrayList<Path>();
        for (final Column col: ptCols) {
            colPathList.add(col.getPath());
        }

        if (pathCollection != null && colPathList.containsAll(pathCollection)) {
            for (final Path p : pathCollection) {
                headers.add(WebUtil.formatPath(p, webConfig));
            }
        } else {
            for (final Column col: pt.getColumns()) {
                headers.add(WebUtil.formatPath(col.getPath(), webConfig));
            }
        }

        return headers;
    }

    /**
     * The initial export path list is just the paths from the columns of the PagedTable.
     * {@inheritDoc}
     */
    @Override
    public List<Path> getInitialExportPaths(final PagedTable pt) {
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
