package org.intermine.webservice.server.exceptions;

/*
 * Copyright (C) 2002-2012 FlyMine
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

    private int httpErrorCode = Output.SC_INTERNAL_SERVER_ERROR;

    private static final long serialVersionUID = 1L;

    /**
     * @param message message
     */
    public ServiceException(String message) {
        super(message);
    }

    /**
     * @param message message
     * @param cause cause
     */
    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause cause
     */
    public ServiceException(Throwable cause) {
        super(cause);
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

    /**
     * @param httpErrorCode http error code
     */
    public void setHttpErrorCode(int httpErrorCode) {
        this.httpErrorCode = httpErrorCode;
    }
}
