package org.intermine.web.util;

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
import java.net.URLEncoder;


/**
 * Util class for URL processing.
 * @author Jakub Kulaviak
 **/
public final class URLUtil
{

    private URLUtil() {
    }

    /**
     * Encodes URL. Parses url and encodes parameter names and
     * parameter values to be safe URL.
     *
     * @param url url
     * @return encoded url
     */
    public static String encodeURL(String url) {
        String[] paths = url.split("\\?");
        String ret = paths[0] + "?";
        if (paths.length == 2) {
            ret += encodeParameters(paths[1]);
        }
        return ret;
    }

    private static String encodeParameters(String queryString) {
        String ret = "";
        String[] pars = queryString.split("&");
        for (int i = 0; i < pars.length; i++) {
            String par = pars[i];
            String[] parts = par.split("=");
            if (parts.length > 1) {
                ret += encodeString(parts[0]);
            }
            if (parts.length == 2) {
                ret += "=" + encodeString(parts[1]);
            }
            if (i != pars.length - 1) {
                ret += "&";
            }
        }
        return ret;
    }

    /**
     * Encodes string to be URL safe. For encoding URL use encodeURL
     * method.
     *
     * @param s string to be encoded
     * @return encoded string
     */
    public static String encodeString(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("string encoding failed", e);
        }
    }
}
