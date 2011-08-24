package org.intermine.webservice.server.query.result;

/*
 * Copyright (C) 2002-2011 FlyMine
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
import org.apache.log4j.Logger;
import org.intermine.webservice.server.exceptions.BadRequestException;

/**
 * Processes service request. Evaluates parameters and validates them and check if
 * its combination is valid.
 *
 * @author Jakub Kulaviak
 **/
public class QueryResultRequestParser extends WebServiceRequestParser
{
    /** Name of parameter with query **/
    public static final String QUERY_PARAMETER = "query";

    /** Compute total count parameter name. **/
    public static final String COMPUTE_TOTAL_COUNT_PARAMETER = "tcount";

    /** Layout parameter name. **/
    public static final String LAYOUT_PARAMETER = "layout";
    
    private static final Logger logger = Logger.getLogger(QueryResultRequestParser.class);

    private HttpServletRequest request;

    /**
     * RequestProcessor constructor.
     * @param request request
     */
    public QueryResultRequestParser(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * Returns parsed parameters in parameter object - so this
     * values can be easily get from this object.
     * @return web service input
     */
    public QueryResultInput getInput() {
        QueryResultInput input = new QueryResultInput();
        parseRequest(request, input);
        return input;
    }

    /**
     * Function for dealing with encoding issues with various
     * inputs.
     */
    public static String fixEncoding(String latin1) {
        try {
            byte[] bytes = latin1.getBytes("ISO-8859-1");
            if (!validUTF8(bytes))
                return latin1;   
            return new String(bytes, "UTF-8");  
        } catch (UnsupportedEncodingException e) {
            // Impossible, throw unchecked
            throw new IllegalStateException("No Latin1 or UTF-8: " + e.getMessage());
        }

    }

    public static boolean validUTF8(byte[] input) {
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

    private void parseRequest(HttpServletRequest req, QueryResultInput input) {

        super.parseRequest(req, input);

        String xmlQuery = req.getParameter(QUERY_PARAMETER);
        logger.debug(xmlQuery);
        xmlQuery = fixEncoding(xmlQuery);
        logger.debug(xmlQuery);
        if (StringUtils.isEmpty(xmlQuery)) {
            throw new BadRequestException("invalid " + QUERY_PARAMETER
                    + " parameter (empty or missing)");
        }
        input.setXml(xmlQuery);

        String totalCount = req.getParameter(COMPUTE_TOTAL_COUNT_PARAMETER);
        if (totalCount != null) {
            throw new BadRequestException("Parameter " + COMPUTE_TOTAL_COUNT_PARAMETER
                    + " is not now supported. It is not possible to retrieve number of results.");
        }

        input.setLayout(req.getParameter(LAYOUT_PARAMETER));
    }
}
