package org.intermine.webservice.output;

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


/**
 * Implementation of <tt>Output</tt> that saves data to memory. 
 * So all the data can be retrieved later.
 * @author Jakub Kulaviak
 **/
public class MemoryOutput extends Output
{

    private List<String> errors = new ArrayList<String>();
    
    private List<List<String>> results = new  ArrayList<List<String>>();

    /** Constructor **/
    public MemoryOutput() { }

    /** Add error messages to memory. 
     * @param errors error messages
     * @param statusCode status code of web service that should be returned
     * */
    @Override
    public void addErrors(List<String> errors, int statusCode) {
        this.errors.addAll(errors);
        setStatus(statusCode);
    }

    /** Add result  item to memory.
     * @param item data
     *  **/
    @Override
    public void addResultItem(List<String> item) {
        results.add(item);
    }

    /** Returns error messages added to output. 
     * @return errors
     */
    public List<String> getErrors() {
        return errors;
    }

    /** Returns results added to output 
     * @return results 
     */
    public List<List<String>> getResults() {
        return results;
    }

    /**
     * Does nothing because MemoryOutput serves as a storage of results during execution. 
     */
    @Override
    public void flush() { }
    
    /**
     * {@inheritDoc}}
     */
    protected int getErrorsCount() {
        return errors.size();
    }
    
    /**
     * {@inheritDoc}}
     */
    protected int getResultsCount() {
        return results.size();
    }
}
