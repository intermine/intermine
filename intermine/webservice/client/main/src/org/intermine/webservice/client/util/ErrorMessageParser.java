package org.intermine.webservice.client.util;

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
 * Parses service error message.
 *
 * @author Jakub Kulaviak
 **/
public final class ErrorMessageParser
{

    private ErrorMessageParser() {
        // Hidden constructor.
    }

    /**
     * Parses service error message.
     * @param errorStr string with error wrapped inside of tags
     * @return error
     */
    public static String parseError(String errorStr) {
        int pos = errorStr.indexOf("</error>");
        if (pos == -1) {
            return "";
        }
        String str = errorStr.substring(0, pos);
        str = str.replaceAll("<error>", " ");
        str = str.replaceAll("<message>", "");
        str = str.replaceAll("</message>", " ");
        str = str.trim();
        return str;
    }
}
