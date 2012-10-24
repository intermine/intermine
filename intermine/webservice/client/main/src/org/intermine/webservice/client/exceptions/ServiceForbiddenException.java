package org.intermine.webservice.client.exceptions;

/*
 * Copyright (C) 2002-2012 FlyMine
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
 * The ServiceForbiddenException is thrown by a service when an
 * attempt is made to service that is disabled.
 *
 * @author Jakub Kulaviak
 */
public class ServiceForbiddenException extends ServiceException
{

    private static final long serialVersionUID = 1L;

    /**
     * @param message message
     */
    public ServiceForbiddenException(String message) {
        super(message);
    }

    /**
     * @param message message
     * @param cause cause
     */
    public ServiceForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause cause
     */
    public ServiceForbiddenException(Throwable cause) {
        super(cause);
    }

    /**
     * @param connection connection
     * @see ServiceException for detailed description
     */
    public ServiceForbiddenException(HttpConnection connection) {
        super(connection);
    }

    @Override
    public int getHttpErrorCode() {
        return HttpURLConnection.HTTP_FORBIDDEN;
    }
}
