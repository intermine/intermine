package org.intermine.webservice.server.query;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import org.intermine.webservice.server.WebServiceRequestParser;
import org.intermine.webservice.server.exceptions.BadRequestException;

/**
 * Processes query request parameters. The main function of this
 * abstraction to to ensure the Query-xml is properly
 * decoded.
 *
 * @author Alex Kalderimis
 **/
public class QueryRequestParser extends WebServiceRequestParser
{

    protected HttpServletRequest request;

    /**
     * RequestProcessor constructor.
     * @param request request
     */
    public QueryRequestParser(HttpServletRequest request) {
        this.request = request;
    }

    private static final String QUERY_PARAMETER = "query";

    /**
     * Function for dealing with encoding issues with various
     * inputs.
     * @param latin1 XML in latin1 encoding.
     * @return The fixed string.
     */
    private static String fixEncoding(String latin1) {
        try {
            byte[] bytes = latin1.getBytes("ISO-8859-1");
            if (!validUTF8(bytes)) {
                return latin1;
            }
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Impossible, throw unchecked
            throw new IllegalStateException("Neither Latin1 nor UTF-8: " + e.getMessage());
        }

    }

    private static boolean validUTF8(byte[] input) {
        int i = 0;
        // Check for BOM
        if (input.length >= 3 && (input[0] & 0xFF) == 0xEF
                && (input[1] & 0xFF) == 0xBB & (input[2] & 0xFF) == 0xBF) {
            i = 3;
        }

        int end;
        for (int j = input.length; i < j; ++i) {
            int octet = input[i];
            if ((octet & 0x80) == 0) {
                continue; // ASCII
            }

            // Check for UTF-8 leading byte
            if ((octet & 0xE0) == 0xC0) {
                end = i + 1;
            } else if ((octet & 0xF0) == 0xE0) {
                end = i + 2;
            } else if ((octet & 0xF8) == 0xF0) {
                end = i + 3;
            } else {
                // Java only supports BMP so 3 is max
                return false;
            }

            while (i < end) {
                i++;
                octet = input[i];
                if ((octet & 0xC0) != 0x80) {
                    // Not a valid trailing byte
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Get query XML from a request.
     * @param req The request to get the XML from.
     * @return The XML string version of the query, in the correct encoding.
     */
    public static String getQueryXml(HttpServletRequest req) {
        String xmlQuery = req.getParameter(QUERY_PARAMETER);
        if (StringUtils.isBlank(xmlQuery)) {
            throw new BadRequestException("The 'query' parameter must not be blank");
        }
        return fixEncoding(xmlQuery);
    }
}
