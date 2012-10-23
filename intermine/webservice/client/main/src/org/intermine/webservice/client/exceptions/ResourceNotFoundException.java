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
 * The ResourceNotFoundException is thrown by a service provider when an
 * attempt is made to perform an operation upon a resource that
 * cannot be found.
 *
 * @author Jakub Kulaviak
 */
public class ResourceNotFoundException extends ServiceException
{

    private static final long serialVersionUID = 1L;

    /**
     * @param message message
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * @param message message
     * @param cause cause
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause cause
     */
    public ResourceNotFoundException(Throwable cause) {
        super(cause);
    }

    /**
     * @param connection connection
     * @see ServiceException for detailed description
     */
    public ResourceNotFoundException(HttpConnection connection) {
        super(connection);
    }

    @Override
    public int getHttpErrorCode() {
        return HttpURLConnection.HTTP_NOT_FOUND;
    }
}
