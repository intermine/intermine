package org.intermine.web.util;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple class that parses provided url. Contains methods that return
 * base url (without query string) and request parameters from url.
 * @author Jakub Kulaviak
 **/
public class AttributeLinkURL
{

    private String baseUrl;

    private Map<String, String> parameters = new HashMap<String, String>();

    /**
     * Constructor.
     * @param urlString parsed url
     * @throws MalformedURLException if url is invalid
     */
    public AttributeLinkURL(String urlString) throws MalformedURLException {
        // Just validates that it is valid url
        String[] parts = urlString.split("\\?", 2);
        baseUrl = parts[0];
        if (parts.length == 2) {
            parameters = parseParameters(parts[1]);
        }
    }

    /**
     * @return original URL without query string and without '?'
     */
    public String getBaseURL() {
        return baseUrl;
    }

    /**
     * @return parameters of original URL
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    private Map<String, String> parseParameters(String queryString) {
        String[] parts = queryString.split("&");
        Map<String, String> ret = new HashMap<String, String>();
        for (String part : parts) {
            part = part.trim();
            if (part.length() > 0) {
                String[] a = part.split("=", 2);
                String value;
                if (a.length == 2) {
                    value = a[1];
                } else {
                    value = "";
                }
                ret.put(a[0], value);
            }
        }
        return ret;
    }
}
