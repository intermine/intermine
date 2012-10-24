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
 * The ServiceForbiddenException is thrown by a service when an attempt is made to service that
 * is disabled.
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
        initResponseCode();
    }

    /**
     * @param message message
     * @param cause cause
     */
    public ServiceForbiddenException(String message, Throwable cause) {
        super(message, cause);
        initResponseCode();
    }

    /**
     * @param cause cause
     */
    public ServiceForbiddenException(Throwable cause) {
        super(cause);
        initResponseCode();
    }

    private void initResponseCode() {
        setHttpErrorCode(Output.SC_FORBIDDEN);
    }
}
