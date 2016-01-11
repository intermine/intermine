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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * The URLParse class is util class for parsing URL.
 *
 * @author Jakub Kulaviak
 **/
public final class URLParser
{

    private URLParser() {
        // Hidden constructor.
    }

    /**
     * Returns service URL - i.e. URL before question mark.
     * @param url URL
     * @return service url
     * @throws MalformedURLException when URL is invalid
     */
    public static String parseServiceUrl(String url) throws MalformedURLException {
        URL webUrl = new URL(url);
        String port;
        if (webUrl.getPort() != -1) {
            port = ":" + webUrl.getPort();
        } else {
            port = "";
        }
        return webUrl.getProtocol() + "://" + webUrl.getHost() + port + webUrl.getPath();
    }

    /**
     * Returns parameters of URL as parameter map where key in the map is the parameter name
     * and value is list of parameter values.
     * @param url URL
     * @return parameter map
     * @throws MalformedURLException when URL is invalid
     */
    public static Map<String, List<String>> parseParameterMap(String url)
        throws MalformedURLException {
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        String query = new URL(url).getQuery();
        if (query != null) {
            String[] parts = query.split("&");
            for (String part : parts) {
                if (parseParameterName(part) != null) {
                    addParameter(map, parseParameterName(part), parseParameterValue(part));
                }
            }
        }
        return map;
    }

    private static void addParameter(Map<String, List<String>> map,
            String name, String value) {
        List<String> values = map.get(name);
        if (values == null) {
            values = new ArrayList<String>();
            map.put(name, values);
        }
        values.add(value);
    }

    private static String parseParameterValue(String str) {
        String[] parts = str.split("=", 2);
        if (parts.length == 2) {
            return parts[1];
        } else if (parts.length == 1) {
            return "";
        }
        return null;
    }

    private static String parseParameterName(String str) {
        String[] parts = str.split("=", 2);
        if (parts.length >= 1) {
            return parts[0];
        }
        return null;
    }
}
