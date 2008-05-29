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
import java.util.Map;


/**
 * Abstract class representing output of web service or something else.
 * Data written to output can be streamed to user via http or saved in memory or something else.
 * It depends at implementation.
 * If the data are saved in memory they can be retrieved later. 
 * @author Jakub Kulaviak
 * 
 */
public abstract class Output  
{
       
    // implicit status code is ok, if something goes wrong that service 
    // sets this code to different error status code
    private int statusCode = Output.SC_OK;
    
    private Map<String, String> headerAttributes;

    /**
     * Bad request http status code.
     */
    public static final int SC_BAD_REQUEST = 400;

    /**
     * Internal server http status code.
     */
    public static final int SC_INTERNAL_SERVER_ERROR = 500;

    /**
     * Forbidden http status code.
     */
    public static final int SC_FORBIDDEN = 403;

    /**
     * OK http status code.
     */
    public static final int SC_OK = 200;

    /**
     * No content http status code.
     */
    public static final int SC_NO_CONTENT = 204;
    
    /**
     * Resource not found http status code.
     */
    public static final int SC_NOT_FOUND = 404;
    
    /**
     * Adds data to output.
     * @param item data
     */
    public abstract void addResultItem(List<String> item);

    /**
     * Adds error messages to output.  
     * @param errors error messages
     * @param statusCode status code
     */
    public abstract void addErrors(List<String> errors, int statusCode);

    /**
     * Adds error message to output.
     * @param error error message
     * @param statusCode status code
     */
    public void addError(String error, int statusCode) {
        List<String> errors = new ArrayList<String>();
        errors.add(error);
        addErrors(errors, statusCode);
    }

    /**
     * Flushes output. What it actually does depends at implementation. 
     */
    public abstract void flush();
    
    /**
     * Sets header atributes that are displayed for example in xml header.
     * @param attributes header attributes
     */
    public void setHeaderAttributes(Map<String, String> attributes) {
        this.headerAttributes = attributes;
    }
    
    /**
     * @see #setHeaderAttributes(Map) 
     * @return header attributes
     */
    public Map<String, String>  getHeaderAttributes() {
        return headerAttributes;
    }
 
    /**
     * @return returned status code
     */
    public int getStatus() {
        if (statusCode == Output.SC_OK) {
            if (getErrorsCount() == 0 && getResultsCount() == 0) {
                return Output.SC_NO_CONTENT;
            }             
        } 
        return statusCode;
    }
    
    /**
     * @param code status code
     */
    public void setStatus(int code) {
        this.statusCode = code;
    }
    
    /**
     * @return errors count
     */
    protected abstract int getErrorsCount();

    /**
     * @return number of written results
     */
    protected abstract int getResultsCount();     
}
