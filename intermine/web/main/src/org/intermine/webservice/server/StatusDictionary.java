package org.intermine.webservice.server;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.webservice.server.output.Output;

/**
 * Http status code dictionary.
 * @author Jakub Kulaviak
 **/
public class StatusDictionary
{
    /**
     * @param statusCode status code
     * @return short description of specified status code
     */
    public static String getDescription(int statusCode) {
        switch (statusCode) {
            case Output.SC_BAD_REQUEST:
                return "There is a problem on the client side (in the browser). Bad request. ";
            case Output.SC_FORBIDDEN:
                return "Forbidden. ";
            case Output.SC_INTERNAL_SERVER_ERROR:
                return "Internal server error. ";
            case Output.SC_NO_CONTENT:
                return "Resource representation is empty. ";
            case Output.SC_NOT_FOUND:
                return "Resource not found. ";
            case Output.SC_OK:
                return "OK";
            default:
                return "";
        }
    }
}
