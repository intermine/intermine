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
 * The ResourceNotFoundException is thrown by a service provider when an attempt is made to
 * perform an operation upon a resource that cannot be found.
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
        initErrorCode();
    }

    /**
     * @param message message
     * @param cause cause
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
        initErrorCode();
    }

    /**
     * @param cause cause
     */
    public ResourceNotFoundException(Throwable cause) {
        super(cause);
        initErrorCode();
    }

    private void initErrorCode() {
        setHttpErrorCode(Output.SC_NOT_FOUND);
    }
}
