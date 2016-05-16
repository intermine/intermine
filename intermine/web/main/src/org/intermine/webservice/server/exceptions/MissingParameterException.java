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

/** @author Alex Kalderimis **/
public class MissingParameterException extends BadRequestException
{

    private static final long serialVersionUID = 6084774809682361096L;

    private static final String MESSAGE_FMT = "Missing parameter: '%s'";

    /** @param parameterName the name of the missing parameter. **/
    public MissingParameterException(String parameterName) {
        super(String.format(MESSAGE_FMT, parameterName));
    }

    /**
     * @param parameterName The name of the missing parameter.
     * @param cause The reason we are throwing an exception.
     */
    public MissingParameterException(String parameterName, Throwable cause) {
        super(String.format(MESSAGE_FMT, parameterName), cause);
    }

}
