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

/**
 * @author Alex Kalderimis
 *
 */
public class NotImplementedException extends ServiceException
{
    private static final long serialVersionUID = -1593418347158889396L;

    /**
     * @param location The place where this happened.
     * @param methodName The method we tried to call.
     */
    public NotImplementedException(Class<?> location, String methodName) {
        super(String.format("%s is not defined for %s", methodName, location.getName()));
    }

}
