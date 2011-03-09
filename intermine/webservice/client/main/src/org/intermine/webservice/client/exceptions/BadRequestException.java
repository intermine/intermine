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

import java.net.HttpURLConnection;

import org.intermine.webservice.client.util.HttpConnection;

/**
 * The BadRequestException is thrown by service when there is a problem with the request.
 * Connected service exists but the service has problems with the parameters of request and
 * client should change
 *  the request.
 * @author Jakub Kulaviak
 *
 */
public class BadRequestException extends ServiceException
{

    private static final long serialVersionUID = 1L;

    /**
     * @param message message
     */
    public BadRequestException(String message) {
        super(message);
        initResponseCode();
    }

    /**
     * @param message message
     * @param cause cause
     */
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
        initResponseCode();
    }

    /**
     * @param cause cause
     */
    public BadRequestException(Throwable cause) {
        super(cause);
        initResponseCode();
    }

    /**
     * @param connection connection
     * @see ServiceException for detailed description
     */
    public BadRequestException(HttpConnection connection) {
        super(connection);
    }

    private void initResponseCode() {
        setHttpErrorCode(HttpURLConnection.HTTP_BAD_REQUEST);
    }
}
