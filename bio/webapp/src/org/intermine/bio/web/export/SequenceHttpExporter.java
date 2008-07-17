package org.intermine.bio.web.export;

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

import org.flymine.model.genomic.LocatedSequenceFeature;
import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.Sequence;
import org.flymine.model.genomic.Translation;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.export.ExportException;
import org.intermine.web.logic.export.ExportHelper;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.logic.export.http.TableHttpExporter;
import org.intermine.web.logic.results.PagedTable;

/**
 * An implementation of TableExporter that exports sequence objects using the BioJava sequence and
 * feature writers.
 *
 * @author Kim Rutherford
 */
public class SequenceHttpExporter implements TableHttpExporter
{

    /**
     * Set response proper header.
     * @param response response
     */
    public static void setSequenceExportHeader(HttpServletResponse response) {
        ResponseUtil.setPlainTextHeader(response, "sequence" + StringUtil.uniqueString() + ".txt");
    }

    /**
     * Method called to export a PagedTable object using the BioJava sequence and feature writers.
     * @param pt exported PagedTable
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     */
    public void export(PagedTable pt, HttpServletRequest request,
                                HttpServletResponse response) {

        HttpSession session = request.getSession();
        ObjectStore os =
            (ObjectStore) session.getServletContext().getAttribute(Constants.OBJECTSTORE);
        setSequenceExportHeader(response);

        OutputStream outputStream = null;
        try {
            outputStream = response.getOutputStream();
        } catch (IOException e) {
            throw new ExportException("Export failed.", e);
        }

        // the first column that contains exportable features
        int realFeatureIndex = getFeatureColumnIndex(pt);

        SequenceExporter exporter = new SequenceExporter(os, outputStream,
                realFeatureIndex);

        exporter.export(pt.getRearrangedResults(), pt.getColumns());
        if (exporter.getWrittenResultsCount() == 0) {
            throw new ExportException("Nothing was found for export.");
        }
    }


    private int getFeatureColumnIndex(PagedTable pt) {
        List<Class> clazzes = ExportHelper.getColumnClasses(pt);
        for (int i = 0; i < clazzes.size(); i++) {
            if (Protein.class.isAssignableFrom(clazzes.get(i))
                || LocatedSequenceFeature.class.isAssignableFrom(clazzes.get(i))
                || Sequence.class.isAssignableFrom(clazzes.get(i))
                || Translation.class.isAssignableFrom(clazzes.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canExport(PagedTable pt) {
        return SequenceExporter.canExportStatic(ExportHelper.getColumnClasses(pt));
    }
}
