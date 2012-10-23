package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Map;


/**
 * Abstract class representing an output of a web service.
 * It depends on the output implementation if the written data are streamed to the user via http
 * or saved in the memory.If the data are saved in memory they can be retrieved later.
 *
 * @author Jakub Kulaviak
 */
public abstract class Output
{

    private Map<String, Object> headerAttributes;

    private String errorMessage = null;
    private int status = SC_OK;

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
     * Sets the error message
     * @param message
     */
    public void setError(String message, int code) {
        errorMessage = message;
        status = code;
    }

    /**
     * Gets the error message.
     * @return The error message
     */
    protected String getError() {
        return errorMessage;
    }

    /**
     * Gets the error code
     * @return the HTTP error code
     */
    protected int getCode() {
        return status;
    }

    /**
     * Adds data to output.
     * @param item data
     */
    public abstract void addResultItem(List<String> item);

    /**
     * Flushes output. What it actually does depends at implementation.
     */
    public abstract void flush();

    /**
     * Sets header attributes that are displayed for example in xml header.
     * @param attributes header attributes
     */
    public void setHeaderAttributes(Map<String, Object> attributes) {
        this.headerAttributes = attributes;
    }

    /**
     * @see #setHeaderAttributes(Map)
     * @return header attributes
     */
    public Map<String, Object>  getHeaderAttributes() {
        return headerAttributes;
    }

    /**
     * @return number of written results
     */
    protected abstract int getResultsCount();
}
