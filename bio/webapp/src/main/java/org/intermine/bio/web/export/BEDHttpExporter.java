package org.intermine.bio.web.export;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.intermine.api.results.ExportResultsIterator;
import org.intermine.bio.web.struts.BEDExportForm;
import org.intermine.metadata.StringUtil;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.pathquery.Path;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.export.ExportException;
import org.intermine.web.logic.export.ExportHelper;
import org.intermine.web.logic.export.Exporter;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.logic.export.http.HttpExportUtil;
import org.intermine.web.logic.export.http.HttpExporterBase;
import org.intermine.web.logic.export.http.TableHttpExporter;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.struts.TableExportForm;

/**
 * An implementation of TableHttpExporter that exports LocatedSequenceFeature
 * objects in BED format.
 *
 * @author Fengyuan Hu
 */
public class BEDHttpExporter extends HttpExporterBase implements TableHttpExporter
{
    /**
     * The batch size to use when we need to iterate through the whole result set.
     */
    public static final int BIG_BATCH_SIZE = 10000;

    /**
     * Method called to export a PagedTable object as BED.  The PagedTable can only be exported if
     * there is exactly one SequenceFeature column and the other columns (if any), are simple
     * attributes (rather than objects).
     * {@inheritDoc}
     */
    @Override
    public void export(PagedTable pt, HttpServletRequest request,
            HttpServletResponse response, TableExportForm form,
            Collection<Path> unionPathCollection, Collection<Path> newPathCollection) {
        boolean doGzip = (form != null) && form.getDoGzip();
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();

        String organisms = null;
        boolean makeUcscCompatible = false;
        String trackDescription = null;

        // try to find the organism from the form
        if (form != null && form instanceof BEDExportForm) {
            organisms = ((BEDExportForm) form).getOrgansimString();
            trackDescription = ((BEDExportForm) form).getTrackDescription();
            if ("yes".equals(((BEDExportForm) form).getUcscCompatibleCheck())) {
                makeUcscCompatible = true;
            }
        }

        if (doGzip) {
            ResponseUtil.setGzippedHeader(response, "table" + StringUtil.uniqueString()
                    + ".bed.gz");
        } else {
            setBEDHeader(response);
        }

        List<Integer> indexes = ExportHelper.getClassIndexes(ExportHelper.getColumnClasses(pt),
                SequenceFeature.class);

        // get the project title to be written in BED records
        Properties props = (Properties) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        String sourceName = props.getProperty("project.title");
        String sourceReleaseVersion = props.getProperty("project.releaseVersion");

        if ("".equals(trackDescription) || trackDescription == null) {
            trackDescription = sourceName + " " + sourceReleaseVersion + " Custom Track";
        }

        Exporter exporter;
        try {
            OutputStream out = response.getOutputStream();
            if (doGzip) {
                out = new GZIPOutputStream(out);
            }
            PrintWriter writer = HttpExportUtil.getPrintWriterForClient(request, out);

            exporter = new BEDExporter(writer, indexes, sourceName, organisms,
                    makeUcscCompatible, trackDescription);
            ExportResultsIterator iter = null;
            try {
                iter = getResultRows(pt, request);
                iter.goFaster();

                // path collections are not in use in BED exporter
                exporter.export(iter, unionPathCollection, newPathCollection);
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
        } catch (Exception e) {
            throw new ExportException("Export failed", e);
        }

//        if (exporter.getWrittenResultsCount() == 0) {
//            throw new ExportException("Nothing was found for export");
//        }
    }

    private static void setBEDHeader(HttpServletResponse response) {
        ResponseUtil.setTabHeader(response, "table" + StringUtil.uniqueString() + ".bed");
    }

    /**
     * The intial export path list is just the paths from the columns of the PagedTable.
     * {@inheritDoc}
     */
    @Override
    public List<Path> getInitialExportPaths(PagedTable pt) {
        return ExportHelper.getColumnPaths(pt);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canExport(PagedTable pt) {
        return BEDExporter.canExportStatic(ExportHelper.getColumnClasses(pt));
    }

    /**
     *
     * @param pt PagedTable
     * @return List<Path>
     */
    public List<Path> getExportClassPaths(PagedTable pt) {
        return new ArrayList<Path>();
    }

}
