package org.intermine.webservice.core;

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

import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.webservice.output.Output;

/**
 * Processor of Results object for easy accessing and parsing of results to list of strings.
 * Actually wrapped Results object is something like proxy that can access the database.  
 * Write method is the point where the data are fetched from database and that's why is slow. 
 * Code example:
 * <pre>
 *   MemoryOutput output = new MemoryOutput();
 *   ResultProcessor processor = new ResultProcessor(results, rowParser, firstResult, maxResults);
 *   processor.write(output);        
 * </pre>
 *   
 * @author Jakub Kulaviak
 **/
public class ResultProcessor
{
    
    private Results results;
    
    private int firstResult;
    
    private int maxResults; 
    
    private ResultRowParser rowParser;

    /**
     * ResultProcessor constructor.
     * @param results Results object
     * @param rowParser parser that do parsing from ResultsRow to list of strings 
     * @param firstResult index of first result inclusive
     * @param maxResults maximum number of results
     */
    public ResultProcessor(Results results, ResultRowParser rowParser, int firstResult, 
            int maxResults) {
        this.results = results;
        this.rowParser = rowParser;
        this.firstResult = firstResult;
        this.maxResults = maxResults;
    }
    
    /**
     * Returns Results object.
     * @return Results object
     */
    public Results getResults() {
        return results;
    }

    /**
     * Sets Results object.
     * @param results Results object
     */
    public void setResults(Results results) {
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
                ResultsRow resultsRow = (ResultsRow) results.get(i);
                List<String> stringRow = rowParser.parse(resultsRow);
                output.addResultItem(stringRow);
            } catch (IndexOutOfBoundsException e) {
                break;
            }
        }
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
                ResultsRow resultsRow = (ResultsRow) results.get(i);
                ret.add(rowParser.parse(resultsRow));
            } catch (IndexOutOfBoundsException e) {
                break;
            }
        }        
        return ret;
    }
}
