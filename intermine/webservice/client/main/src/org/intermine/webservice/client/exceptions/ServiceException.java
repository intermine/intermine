package org.intermine.webservice.client.exceptions;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.webservice.client.util.ErrorMessageParser;
import org.intermine.webservice.client.util.HttpConnection;


/**
 * ServiceException is a base class for all service exceptions.
 *
 * @author Jakub Kulaviak
 */
public class ServiceException extends RuntimeException
{

    private int httpErrorCode;

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
     * Exception is filled in with some detailed information from response.
     * @param connection connection
     */
    public ServiceException(HttpConnection connection) {
        super(ErrorMessageParser.parseError(connection.getResponseBodyAsString()));
        setHttpErrorCode(connection.getResponseCode());
    }

     /**
      * {@inheritDoc}
      */
    @Override
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
