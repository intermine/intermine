package org.intermine.bio.web.export;

/*
 * Copyright (C) 2002-2017 FlyMine
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
import org.intermine.api.InterMineAPI;
import org.intermine.api.results.Column;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.bio.web.logic.SequenceFeatureExportUtil;
import org.intermine.bio.web.struts.SequenceExportForm;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.metadata.StringUtil;
import org.intermine.web.logic.export.ExportException;
import org.intermine.web.logic.export.ExportHelper;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.logic.export.http.HttpExporterBase;
import org.intermine.web.logic.export.http.TableHttpExporter;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.TableExportForm;

/**
 * Obsoleted - replaced by new results table
 *
 * An implementation of TableExporter that exports sequence objects using the BioJava sequence and
 * feature writers.
 *
 * @author Kim Rutherford
 */
public class SequenceHttpExporter extends HttpExporterBase implements TableHttpExporter
{
    protected static final Logger LOG = Logger.getLogger(SequenceHttpExporter.class);

    /**
     * Set response proper header.
     * @param response response
     * @param doGzip true if the output should be gzipped
     */
    public static void setSequenceExportHeader(HttpServletResponse response, boolean doGzip) {
        String fileName = "sequence" + StringUtil.uniqueString() + ".fasta";
        if (doGzip) {
            ResponseUtil.setGzippedHeader(response, fileName + ".gz");
        } else {
            ResponseUtil.setCustomTypeHeader(response, fileName, "chemical/x-fasta");
        }
    }

    /**
     * Method called to export a PagedTable object using the BioJava sequence and feature writers.
     * {@inheritDoc}
     */
    @Override
    public void export(PagedTable pt, HttpServletRequest request,
            HttpServletResponse response, TableExportForm form,
            Collection<Path> unionPathCollection, Collection<Path> newPathCollection) {
        boolean doGzip = (form != null) && form.getDoGzip();
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        ObjectStore os = im.getObjectStore();

        setSequenceExportHeader(response, doGzip);

        SequenceExportForm sef = (SequenceExportForm) form;

        OutputStream outputStream = null;
        try {
            outputStream = response.getOutputStream();
            if (doGzip) {
                outputStream = new GZIPOutputStream(outputStream);
            }
        } catch (IOException e) {
            throw new ExportException("Export failed.", e);
        }

        String sequencePathString = null;

        if (sef != null) {
            sequencePathString = sef.getSequencePath();
        }

        if (sequencePathString == null) {
            // fall back case: pick the first sequence object that occurs in the view
            List<Path> sequencePaths =
                SequenceFeatureExportUtil.getExportClassPaths(pt.getPathQuery());
            sequencePathString = sequencePaths.iterator().next().toString();
        }
        sequencePathString = sequencePathString.replaceAll(" > ", ".");

        int realFeatureIndex = 0;

        for (Column column: pt.getColumns()) {
            Path path = column.getPath();

            // need path string without and [] denoting subclasses
            String pathString = path.toStringNoConstraints();
            if (path.endIsAttribute()
                && pathString.startsWith(sequencePathString)
                && !pathString.substring(sequencePathString.length() + 1).contains(".")) {
                realFeatureIndex = column.getIndex();
                break;
            }
        }

        SequenceExporter exporter = new SequenceExporter(os, outputStream, realFeatureIndex,
                im.getClassKeys(), 0);
        ExportResultsIterator iter = null;
        try {
            iter = getResultRows(pt, request);
            iter.goFaster();
            exporter.export(iter, unionPathCollection, newPathCollection);
            if (outputStream instanceof GZIPOutputStream) {
                try {
                    ((GZIPOutputStream) outputStream).finish();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } finally {
            if (iter != null) {
                iter.releaseGoFaster();
            }
        }

//        if (exporter.getWrittenResultsCount() == 0) {
//            throw new ExportException("Nothing was found for export.");
//        }
    }

    /**
     * The intial export path list is just the paths from the columns of the PagedTable with
     * chromosomeLocation added (if appropriate)
     * {@inheritDoc}
     * @throws PathException
     */
    @Override
    public List<Path> getInitialExportPaths(PagedTable pt) throws PathException {
        List<Path> paths = new ArrayList<Path>(ExportHelper.getColumnPaths(pt));

        List<Path> sequencePaths = SequenceFeatureExportUtil.getExportClassPaths(pt.getPathQuery());

        for (Path seqPath: sequencePaths) {
            Class<?> seqPathClass = seqPath.getLastClassDescriptor().getType();
            if (SequenceFeature.class.isAssignableFrom(seqPathClass)) {
                // skip chromosome class, so ...chromosome.chromosomeLocation doesn't appear in
                // paths, because chromosome.chromosomeLocation is empty and it caused empty
                // export results
                if (Chromosome.class.isAssignableFrom(seqPathClass)) {
                    continue;
                }
                // the Path we need is the parent of one of the paths in the columns
                // Broken in 0.94
                // paths.add(seqPath.append("chromosomeLocation"));
            }
        }

        return paths;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canExport(PagedTable pt) {
        return SequenceExporter.canExportStatic(ExportHelper.getColumnClasses(pt));
    }
}
