package org.intermine.webservice.client.exceptions;

/*
 * Copyright (C) 2002-2016 FlyMine
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
 * The ServiceUnavailableException is thrown when an exception occurs that
 * is temporary, the user should try it again.
 *
 * @author Jakub Kulaviak
 */
public class ServiceUnavailableException extends ServiceException
{

    private static final long serialVersionUID = 1L;

    /**
     * @param message message
     */
    public ServiceUnavailableException(String message) {
        super(message);
    }

    /**
     * @param message message
     * @param cause cause
     */
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     *
     * @param cause cause
     */
    public ServiceUnavailableException(Throwable cause) {
        this(null, cause);
    }

    /**
     * @param connection connection
     * @see ServiceException for detailed description
     */
    public ServiceUnavailableException(HttpConnection connection) {
        super(connection);
    }

    @Override
    public int getHttpErrorCode() {
        return HttpURLConnection.HTTP_UNAVAILABLE;
    }
}
