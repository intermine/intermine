package org.intermine.webservice.server.exceptions;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.webservice.server.output.Output;


/**
 * ServiceException is a base class for all service exceptions.
 *
 * @author Jakub Kulaviak
 */
public class ServiceException extends RuntimeException
{

    private final int httpErrorCode; //  = Output.SC_INTERNAL_SERVER_ERROR;

    private static final long serialVersionUID = 1L;

    /**
     * @param message message
     */
    public ServiceException(String message) {
        super(message);
        httpErrorCode = Output.SC_INTERNAL_SERVER_ERROR;
    }

    /**
     * @param message message
     * @param code The error code.
     */
    public ServiceException(String message, int code) {
        super(message);
        httpErrorCode = code;
    }

    /**
     * @param message message
     * @param cause cause
     */
    public ServiceException(String message, Throwable cause) {
        super(message, cause);
        httpErrorCode = Output.SC_INTERNAL_SERVER_ERROR;
    }

    /**
     * @param message message
     * @param cause cause
     * @param code the error code.
     */
    public ServiceException(String message, Throwable cause, int code) {
        super(message, cause);
        httpErrorCode = code;
    }

    /**
     * @param cause cause
     */
    public ServiceException(Throwable cause) {
        super(cause);
        httpErrorCode = Output.SC_INTERNAL_SERVER_ERROR;
    }

    /**
     * @param cause cause
     * @param code the error code.
     */
    public ServiceException(Throwable cause, int code) {
        super(cause);
        httpErrorCode = code;
    }

     /**
      * {@inheritDoc}
      */
    public String getMessage() {
        if (super.getMessage() == null || super.getMessage().length() == 0) {
            return "Error code: " + getHttpErrorCode();
        } else {
            return super.getMessage();
        }
    }
    /**
     * @return http error code
     */
    public int getHttpErrorCode() {
        return httpErrorCode;
    }

}
