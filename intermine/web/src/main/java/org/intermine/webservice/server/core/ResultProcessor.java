package org.intermine.webservice.server.core;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.intermine.api.results.ResultElement;
import org.intermine.pathquery.ConstraintValueParser;
import org.intermine.webservice.server.output.Output;

/**
 * Processor of Results object for easy accessing and parsing of results to list of strings.
 * Actually wrapped Results object is something like proxy that can access the database.
 * Write method is the point where the data are fetched from database and that's why is slow.
 * Code example:
 * <pre>
 *   MemoryOutput output = new MemoryOutput();
 *   ResultProcessor processor = new ResultProcessor(results, firstResult, maxResults);
 *   processor.write(output);
 * </pre>
 *
 * @author Jakub Kulaviak
 **/
public class ResultProcessor
{
    /**
     * Constructor.
     */
    public ResultProcessor() {
        // Nothing to do.
    }

    /**
     * Writes results to output.
     * @param resultIt iterator over results row
     * @param output output
     */
    public void write(Iterator<List<ResultElement>> resultIt, Output output) {
        while (resultIt.hasNext())  {
            List<ResultElement> row = resultIt.next();
            output.addResultItem(convertResultElementsToStrings(row));
        }
    }

    private static List<String> convertResultElementsToStrings(List<ResultElement> row) {
        List<String> ret = new ArrayList<String>();
        String value;
        for (ResultElement el : row) {
            if (el != null && el.getField() != null) {
                if (el.getField() instanceof Date) {
                    value = ConstraintValueParser.ISO_DATE_FORMAT.format(el.getField());
                } else {
                    value = el.getField().toString();
                }
            } else {
                value = "";
            }
            ret.add(value);
        }
        return ret;
    }
}
