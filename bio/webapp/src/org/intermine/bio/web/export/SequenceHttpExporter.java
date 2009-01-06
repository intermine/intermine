package org.intermine.bio.web.export;

/*
 * Copyright (C) 2002-2009 FlyMine
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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.LocatedSequenceFeature;
import org.intermine.bio.web.struts.SequenceExportForm;
import org.intermine.bio.web.struts.SequenceExportOptionsController;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Path;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.export.ExportException;
import org.intermine.web.logic.export.ExportHelper;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.logic.export.http.HttpExporterBase;
import org.intermine.web.logic.export.http.TableHttpExporter;
import org.intermine.web.logic.results.Column;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.struts.TableExportForm;

/**
 * An implementation of TableExporter that exports sequence objects using the BioJava sequence and
 * feature writers.
 *
 * @author Kim Rutherford
 */
public class SequenceHttpExporter extends HttpExporterBase implements TableHttpExporter
{
    /**
     * Set response proper header.
     * @param response response
     */
    public static void setSequenceExportHeader(HttpServletResponse response) {
        String fileName = "sequence" + StringUtil.uniqueString() + ".fasta";
        ResponseUtil.setPlainTextHeader(response, fileName);
    }

    /**
     * Method called to export a PagedTable object using the BioJava sequence and feature writers.
     * {@inheritDoc}
     */
    public void export(PagedTable pt, HttpServletRequest request, HttpServletResponse response,
                       TableExportForm form) {

        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        setSequenceExportHeader(response);

        SequenceExportForm sef = (SequenceExportForm) form;

        OutputStream outputStream = null;
        try {
            outputStream = response.getOutputStream();
        } catch (IOException e) {
            throw new ExportException("Export failed.", e);
        }

        String sequencePathString = sef.getSequencePath();
        sequencePathString = sequencePathString.replaceAll(" > ", ".");

        int realFeatureIndex = 0;

        for (Column column: pt.getColumns()) {
            Path path = column.getPath();

            String pathString = path.toString();
            if (path.endIsAttribute()
                && pathString.startsWith(sequencePathString)
                && !pathString.substring(sequencePathString.length() + 1).contains(".")) {
                realFeatureIndex = column.getIndex();
                break;
            }
        }

        SequenceExporter exporter = new SequenceExporter(os, outputStream, realFeatureIndex);
        try {
            exporter.export(getResultRows(pt, request));    
        } finally {
            releaseGoFaster();
        }
        
        if (exporter.getWrittenResultsCount() == 0) {
            throw new ExportException("Nothing was found for export.");
        }
    }

    /**
     * The intial export path list is just the paths from the columns of the PagedTable with
     * chromosomeLocation added (if appropriate)
     * {@inheritDoc}
     */
    public List<Path> getInitialExportPaths(PagedTable pt) {
        List<Path> paths = new ArrayList<Path>(ExportHelper.getColumnPaths(pt));

        List<Path> sequencePaths = SequenceExportOptionsController.getExportClassPaths(pt);

        for (Path seqPath: sequencePaths) {
            Class<?> seqPathClass = seqPath.getEndClassDescriptor().getType();
            if (LocatedSequenceFeature.class.isAssignableFrom(seqPathClass)) {
                // skip chromosome class, so ...chromosome.chromosomeLocation doesn't appear in
                // paths, because chromosome.chromosomeLocation is empty and it caused empty
                // export results
                if (Chromosome.class.isAssignableFrom(seqPathClass)) {
                    continue;
                }
                // the Path we need is the parent of one of the paths in the columns
                paths.add(seqPath.append("chromosomeLocation"));
            }
        }

        return paths;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canExport(PagedTable pt) {
        return SequenceExporter.canExportStatic(ExportHelper.getColumnClasses(pt));
    }
}
