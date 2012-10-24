package org.intermine.webservice.exceptions;

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
 * The InternalErrorException is thrown by service when there is an internal error.
 *
 * @author Jakub Kulaviak
 */
public class InternalErrorException extends ServiceException
{

    private static final long serialVersionUID = 1L;

    /**
     * @param message message
     */
    public InternalErrorException(String message) {
        super(message);
        initErrorCode();
    }

    /**
     * @param message message
     * @param cause cause
     */
    public InternalErrorException(String message, Throwable cause) {
        super(message, cause);
        initErrorCode();
    }

    /**
     * @param cause cause
     */
    public InternalErrorException(Throwable cause) {
        super(cause);
        initErrorCode();
    }

    private void initErrorCode() {
        setHttpErrorCode(Output.SC_INTERNAL_SERVER_ERROR);
    }
}
