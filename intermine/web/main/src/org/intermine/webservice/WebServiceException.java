package org.intermine.webservice;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Base runtime exception that can be thrown during web service execution.
 * @author Jakub Kulaviak
 **/
public class WebServiceException extends RuntimeException
{

    private static final long serialVersionUID = 1L;


    /**
     * WebServiceException constructor.
     * @param message message
     */
    public WebServiceException(String message) {
        super(message);
    }

    /**
     * WebServiceException constructor.
     * @param message message
     * @param ex cause
     */
    public WebServiceException(String message, Exception ex) {
        super(message, ex);
    }
}
