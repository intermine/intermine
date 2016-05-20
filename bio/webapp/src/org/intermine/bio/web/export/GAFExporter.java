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

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.intermine.api.results.ResultElement;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.pathquery.Path;
import org.intermine.web.logic.export.ExportHelper;
import org.intermine.web.logic.export.Exporter;

/**
 * Exports LocatedSequenceFeature objects to GO Annotation File Format 2.0
 *
 * @author Fengyuan Hu
 */
public class GAFExporter implements Exporter
{
    // Format guide: http://www.geneontology.org/GO.format.gaf-2_0.shtml

    PrintWriter out;
    private int writtenCount = 0;
    private List<Integer> featureIndexes;
    private Collection<String> taxonIds;

    private static final String HEADER = "!gaf-version: 2.0";

    /**
     * Constructor.
     * @param out output stream
     * @param featureIndexes index of column with exported sequence
     * @param taxonIds taxonIDs to export
     */
    public GAFExporter(PrintWriter out, List<Integer> featureIndexes, Collection<String> taxonIds) {
        this.out = out;
        this.featureIndexes = featureIndexes;
        this.taxonIds = taxonIds;
    }

    @Override
    public void export(Iterator<? extends List<ResultElement>> it,
            Collection<Path> unionPathCollection,
            Collection<Path> newPathCollection) {
        // TODO Auto-generated method stub

    }

    @Override
    public void export(Iterator<? extends List<ResultElement>> resultIt) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean canExport(List<Class<?>> clazzes) {
        return canExportStatic(clazzes);
    }

    /* Method must have different name than canExport because canExport() method
     * is inherited from Exporter interface */
    /**
     * @param clazzes classes of result row
     * @return true if this exporter can export result composed of specified classes
     */
    public static boolean canExportStatic(List<Class<?>> clazzes) {
        return ExportHelper.getClassIndex(clazzes, SequenceFeature.class) >= 0;
    }

    @Override
    public int getWrittenResultsCount() {
        return writtenCount;
    }

}
