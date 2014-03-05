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

public class MissingParameterException extends BadRequestException {

    private static final long serialVersionUID = 6084774809682361096L;
	
	private static final String MESSAGE_FMT = "Missing parameter: '%s'";

    public MissingParameterException(String parameterName) {
        super(String.format(MESSAGE_FMT, parameterName));
    }

    public MissingParameterException(String parameterName, Throwable cause) {
        super(String.format(MESSAGE_FMT, parameterName), cause);
    }

}
