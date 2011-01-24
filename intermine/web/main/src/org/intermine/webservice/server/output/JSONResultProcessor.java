package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.webservice.server.core.ResultProcessor;

/**
 * An class that defines the basic methods for processing JSON results.
 * It does however not define how the JSON results themselves are processed.
 * @author Alexis Kalderimis
 *
 */
public abstract class JSONResultProcessor extends ResultProcessor
{

    /**
     * Constructor.
     */
    public JSONResultProcessor() {
        // Empty constructor
    }

    /**
     * The method a processor must implement to produce a results iterator.
     * @param it The ExportResultsIterator this iterator will use to process its data.
     * @return An iterator of objects.
     */
    protected abstract Iterator<? extends Object> getResultsIterator(ExportResultsIterator it);

    @SuppressWarnings("unchecked")
    @Override
    public void write(Iterator<List<ResultElement>> resultIt, Output output) {
        if (!(resultIt instanceof ExportResultsIterator)) {
            throw new IllegalArgumentException("The iterator must be an ExportResultsIterator");
        }
        ExportResultsIterator exportIter = (ExportResultsIterator) resultIt;
        Iterator<? extends Object> objIter = getResultsIterator(exportIter);
        if (!objIter.hasNext()) { // address bug which means json results with < 1 results fail
            output.addResultItem(Collections.EMPTY_LIST);
        }
        while (objIter.hasNext()) {
            Object next = objIter.next();
            List<String> outputLine = new ArrayList<String>(
                    Arrays.asList(next.toString()));
            if (objIter.hasNext()) { outputLine.add(""); }
            output.addResultItem(outputLine);
        }
    }

}
