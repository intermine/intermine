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

/** @author Alex Kalderimis **/
public class UnauthorizedException extends ServiceException
{

    private static final long serialVersionUID = 1L;
    private static final int ERROR_CODE = Output.SC_UNAUTHORIZED;

    /** Construct an UnauthorizedException **/
    public UnauthorizedException() {
        super("This service requires authentication.", ERROR_CODE);
    }

    /** @param message A description of the problem **/
    public UnauthorizedException(String message) {
        super(message, ERROR_CODE);
    }
}
