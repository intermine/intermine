package org.intermine.bio.web.export;

/*
 * Copyright (C) 2002-2012 FlyMine
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

import org.apache.log4j.Logger;
import org.intermine.api.results.ResultElement;
import org.intermine.pathquery.Path;
import org.intermine.web.logic.export.Exporter;

/**
 * Exports LocatedSequenceFeature objects to GO Annotation File Format 2.0
 *
 * @author Fengyuan Hu
 */
public class GAFExporter implements Exporter
{
    // Format guide: http://www.geneontology.org/GO.format.gaf-2_0.shtml

    private static final Logger LOG = Logger.getLogger(GAFExporter.class);

    PrintWriter out;
    private List<Integer> featureIndexes;
    private String taxonIds;

    private static final String HEADER ="!gaf-version: 2.0";

    /**
     * Constructor.
     * @param out output stream
     * @param featureIndexes index of column with exported sequence
     */
    public GAFExporter(PrintWriter out, List<Integer> featureIndexes, String taxonIds) {
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
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getWrittenResultsCount() {
        // TODO Auto-generated method stub
        return 0;
    }

}
