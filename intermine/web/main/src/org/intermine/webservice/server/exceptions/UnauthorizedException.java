package org.intermine.webservice.server.exceptions;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.webservice.server.output.Output;

public class UnauthorizedException extends ServiceException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedException() {
        super("This service requires authentication.");
        initResponseCode();
    }

    public UnauthorizedException(String message) {
        super(message);
        initResponseCode();
    }

    private void initResponseCode() {
        setHttpErrorCode(Output.SC_UNAUTHORIZED);
    }
}
