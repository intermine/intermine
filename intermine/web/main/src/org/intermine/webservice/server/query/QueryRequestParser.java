package org.intermine.webservice.server.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.UnsupportedEncodingException;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import org.intermine.api.query.QueryStore;
import org.intermine.api.query.QueryStoreException;
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

    private QueryStore queryStore;

    /**
     * RequestProcessor constructor.
     * @param request request
     * @param queryStore a place to retrieve queries by qid.
     */
    public QueryRequestParser(QueryStore queryStore, HttpServletRequest request) {
        this.queryStore = queryStore;
        this.request = request;
    }

    private static final String QUERY_PARAMETER = "query";

    /**
     * The query parameter for queries passed as a
     * lzw compressed string.
     */
    private static final String QLZW_PARAMETER = "qlzw";

    private static final String QID = "qid";

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
     * Decompress a list of output ks to a string.
     *
     * Gratefully nicked from Stack-Overflow.
     * @param compressed A query compressed to a list of bytes.
     * @return The decompressed query.
     **/
    public static String decompressLZW(List<Integer> compressed) {
        // Build the dictionary.
        int dictSize = 256;
        Map<Integer, String> dictionary = new HashMap<Integer, String>();
        for (int i = 0; i < 256; i++) {
            dictionary.put(i, "" + (char) i);
        }

        String w = "" + (char) (int) compressed.remove(0);
        String result = w;
        for (int k : compressed) {
            String entry;
            if (dictionary.containsKey(k)) {
                entry = dictionary.get(k);
            } else if (k == dictSize) {
                entry = w + w.charAt(0);
            } else {
                throw new IllegalArgumentException("Bad compressed k: " + k);
            }

            result += entry;

            // Add w+entry[0] to the dictionary.
            dictionary.put(dictSize++, w + entry.charAt(0));

            w = entry;
        }
        return result;
    }

    /**
     * Take in a LZW encoded string and return a decoded plain-text string.
     * @return the decompressed query.
     * @param encoded The compressed and encoded representation of the query.
     */
    public static String decodeLZWString(String encoded) {
        List<Integer> codes = new ArrayList<Integer>();
        encoded = fixEncoding(encoded);
        int length = encoded.length();
        for (int i = 0; i < length; i++) {
            Integer cp = Integer.valueOf(encoded.codePointAt(i));
            codes.add(cp);
        }
        return decompressLZW(codes);
    }

    /**
     * Get query XML from a request.
     * @return The XML string version of the query, in the correct encoding.
     */
    public String getQueryXml() {
        String xmlQuery, lzwQuery, qid;
        qid = request.getParameter(QID);
        xmlQuery = request.getParameter(QUERY_PARAMETER);
        lzwQuery = request.getParameter(QLZW_PARAMETER);

        if (StringUtils.isNotBlank(qid)) {
            try {
                return queryStore.getQuery(qid);
            } catch (QueryStoreException e) {
                throw new BadRequestException(e.getMessage());
            }
        } else if (StringUtils.isNotBlank(lzwQuery)) {
            xmlQuery = decodeLZWString(lzwQuery);
        }
        if (StringUtils.isBlank(xmlQuery)) {
            throw new BadRequestException("The 'query' parameter must not be blank");
        }
        return fixEncoding(xmlQuery);
    }
}
