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
import java.util.List;

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
        int end = maxResults + firstResult;
        for (int i = firstResult; i < end; i++) {
            try {
                List<ResultElement> row = results.getResultElements(i);
                output.addResultItem(convertResultElementsToStrings(row));
            } catch (IndexOutOfBoundsException e) {
                // At the end of results
                break;
            }
        }
    }

    private List<String> convertResultElementsToStrings(List<ResultElement> row) {
        List<String> ret = new ArrayList<String>();
        for (ResultElement el : row) {
            if (el != null && el.getField() != null) {
                ret.add(el.getField().toString());    
            }
        }
        return ret;
    }

    /**
     * Returns parsed results, i.e. each ResultsRow as a List<String>. Transformation
     * performs associated parser.
     * @see setFirstResult()
     * @see setMaxResults()
     * @return parsed results
     */
    public List<List<String>> getParsedResults() {
        List<List<String>> ret = new ArrayList<List<String>>();
        int end = maxResults + firstResult;
        for (int i = firstResult; i < end; i++) {
            try {
                List<ResultElement> row = results.getResultElements(i);
                ret.add(convertResultElementsToStrings(row));
            } catch (IndexOutOfBoundsException e) {
                // At the end of results
                break;
            }
        }        
        return ret;
    }
}
