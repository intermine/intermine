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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.flymine.model.genomic.LocatedSequenceFeature;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.bio.util.GFF3Util;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.IntPresentSet;
import org.intermine.web.logic.export.ExportException;
import org.intermine.web.logic.export.ExportHelper;
import org.intermine.web.logic.export.Exporter;
import org.intermine.web.logic.results.ResultElement;

/**
 * Exports LocatedSequenceFeature objects in GFF3 format.
 * @author Kim Rutherford
 * @author Jakub Kulaviak
 */
public class GFF3Exporter implements Exporter
{

    PrintWriter out;
    private List<Integer> featureIndexes;
    private Map<String, String> soClassNames;
    private int writtenResultsCount = 0;
    private boolean headerPrinted = false;
    private IntPresentSet exportedIds = new IntPresentSet();
    private List<String> attributesNames;

    /**
     * Constructor.
     * @param out output stream
     * @param indexes index of column with exported sequence
     * @param soClassNames mapping
     * @param attributesNames names of attributes that are printed in record, 
     *  they are names of columns in results table, they are in the same order
     *  as corresponding columns in results table  
     */
    public GFF3Exporter(PrintWriter out, List<Integer> indexes, Map<String, String> soClassNames, 
            List<String> attributesNames) {
        this.out = out;
        this.featureIndexes = indexes;
        this.soClassNames = soClassNames;
        this.attributesNames = attributesNames;
    }

    /**
     * {@inheritDoc}
     */
    public void export(List<List<ResultElement>> results) {
        if (featureIndexes.size() == 0) {
            throw new ExportException("No columns with sequence");
        }
        try {
            for (int i = 0; i < results.size(); i++) {
                List<ResultElement> row = results.get(i);
                exportRow(row);
            }
            out.flush();
        } catch (Exception ex) {
            throw new ExportException("Export failed", ex);
        }
    }

    private void exportRow(List<ResultElement> row)
        throws ObjectStoreException,
        IllegalAccessException {
        ResultElement elWithObject = getResultElement(row);
        if (elWithObject == null) {
            return;
        }

        LocatedSequenceFeature lsf = (LocatedSequenceFeature) elWithObject.getInterMineObject();

        if (exportedIds.contains(lsf.getId())) {
            return;
        }

        Map<String, List<String>> attributes = new LinkedHashMap<String, List<String>>();
        for (int i = 0; i < row.size(); i++) {
            ResultElement el = row.get(i);
            attributes.put(attributesNames.get(i), formatElementValue(el));
        }
        
        GFF3Record gff3Record = GFF3Util.makeGFF3Record(lsf, soClassNames,
                attributes);

        if (gff3Record == null) {
            // no chromsome ref or no chromosomeLocation ref
            return;
        }

        if (!headerPrinted) {
            out.println("##gff-version 3");
            headerPrinted = true;
        }

        out.println(gff3Record.toGFF3());
        exportedIds.add(lsf.getId());
        writtenResultsCount++;
    }

    private List<String> formatElementValue(ResultElement el) {
        List<String> ret = new ArrayList<String>();
        Object obj = el.getField();
        String s;
        if (obj == null) {
            s = "-";
        } else {
            s = obj.toString();
        }
        ret.add(s);
        return ret;
    }

    private ResultElement getResultElement(List<ResultElement> row) {
        ResultElement el = null;
        for (Integer index : featureIndexes) {
            el = row.get(index);
            if (el != null) {
                break;
            }
        }
        return el;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canExport(List<Class> clazzes) {
        return canExportStatic(clazzes);
    }

    /* Method must have different name than canExport because canExport() method
     * is  inherited from Exporter interface */
    /**
     * @param clazzes classes of result row
     * @return true if this exporter can export result composed of specified classes
     */
    public static boolean canExportStatic(List<Class> clazzes) {
        return ExportHelper.getClassIndex(clazzes, LocatedSequenceFeature.class) >= 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getWrittenResultsCount() {
        return writtenResultsCount;
    }
}
