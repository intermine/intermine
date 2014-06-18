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

public class NotImplementedException extends InternalErrorException
{
    private static final long serialVersionUID = -1593418347158889396L;

    public NotImplementedException(String message) {
        super(message);
    }

    public NotImplementedException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotImplementedException(Throwable cause) {
        super(cause);
    }
    
    public NotImplementedException(Class<?> location, String methodName) {
        super(String.format("%s is not defined for %s", methodName, location.getName()));
    }

}
