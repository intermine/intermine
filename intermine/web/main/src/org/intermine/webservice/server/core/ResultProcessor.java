package org.intermine.webservice.server.core;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.intermine.objectstore.flatouterjoins.ReallyFlatIterator;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.Constraint;
import org.intermine.web.logic.results.ResultElement;
import org.intermine.web.logic.results.WebResults;
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
    
    private WebResults results;
    
    private int firstResult;
    
    private int maxResults; 
    
    /**
     * ResultProcessor constructor.
     * @param results Results object
     * @param firstResult index of first result inclusive
     * @param maxResults maximum number of results
     */
    public ResultProcessor(WebResults results, int firstResult, 
            int maxResults) {
        this.results = results;
        this.firstResult = firstResult;
        this.maxResults = maxResults;
    }
    
    /**
     * Returns Results object.
     * @return Results object
     */
    public WebResults getResults() {
        return results;
    }

    /**
     * Sets Results object.
     * @param results Results object
     */
    public void setResults(WebResults results) {
        this.results = results;
    }

    /**
     * Sets index of result from which the data will be returned.
     * @param firstResult index of first result inclusive
     */
    public void setFirstResult(int firstResult) {
        this.firstResult = firstResult;
    }

    /**
     * Sets maximum number of returned results.
     * @param maxResults maximum number of results
     */
    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    /**
     * Writes results to output.
     * @param output output
     * @see setFirstResult()
     * @see setMaxResults()
     */
    public void write(Output output) {
        ReallyFlatIterator it = new ReallyFlatIterator(results.iteratorFrom(firstResult));
        int writtenCount = 0;
        while (it.hasNext())  {
            if (writtenCount >= maxResults) {
                break;
            }
            ResultsRow row = (ResultsRow) it.next();
            output.addResultItem(convertResultElementsToStrings(row));
            writtenCount++;
        }
    }

    private List<String> convertResultElementsToStrings(List<ResultElement> row) {
        List<String> ret = new ArrayList<String>();
        String value;
        for (ResultElement el : row) {
            if (el != null && el.getField() != null) {
                if (el.getField() instanceof Date) {
                    value = Constraint.ISO_DATE_FORMAT.format(el.getField());    
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
