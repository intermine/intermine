package org.intermine.webservice.server;

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
 * HTTP status code dictionary.
 * @author Jakub Kulaviak
 **/
public abstract class StatusDictionary
{

    private StatusDictionary() {
    }

    /**
     * @param statusCode status code
     * @return short description of specified status code
     */
    public static String getDescription(int statusCode) {
        String ret;
        switch (statusCode) {
            case Output.SC_BAD_REQUEST:
                ret = "Bad request. There was a problem with your request parameters:"; break;
            case Output.SC_FORBIDDEN:
                ret = "Forbidden."; break;
            case Output.SC_INTERNAL_SERVER_ERROR:
                ret = "Internal server error."; break;
            case Output.SC_NO_CONTENT:
                ret = "Resource representation is empty."; break;
            case Output.SC_NOT_FOUND:
                ret = "Resource not found."; break;
            case Output.SC_OK:
                ret = "OK"; break;
            default:
                ret = "Unknown Status";
        }
        return statusCode + " " + ret;
    }
}
