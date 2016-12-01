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
 * The NoContentException is thrown by a service when the resource requested exists but
 * has an empty representation.
 *
 * @author Jakub Kulaviak
 */
public class NoContentException extends ServiceException
{

    private static final long serialVersionUID = 1L;
    private static final int ERROR_CODE = Output.SC_NO_CONTENT;

    /**
     * @param message message
     */
    public NoContentException(String message) {
        super(message, ERROR_CODE);
    }

    /**
     * @param message message
     * @param cause cause
     */
    public NoContentException(String message, Throwable cause) {
        super(message, cause, ERROR_CODE);
    }

    /**
     * @param cause cause
     */
    public NoContentException(Throwable cause) {
        super(cause, ERROR_CODE);
    }
}
