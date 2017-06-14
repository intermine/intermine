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

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.intermine.web.context.InterMineContext;

/**
 * Class generating useful links like base link: http://localhost:8080/query
 * @author Jakub Kulaviak
 **/
public class URLGenerator
{

    private HttpServletRequest request;

    /**
     * Constructor.
     * @param request request
     */
    public URLGenerator(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * Generates base url. If default context path is defined in web.properties, then this
     * path is used, else request context path is used. This enables generation of links to
     * the application and not to the particular version of application.
     * @return base url. For example: http://localhost:8080/query
     */
    public String getPermanentBaseURL() {
        String contextPath = request.getContextPath();
        return generateURL(request, contextPath);
    }

    /**
     * @return base url.
     */
    public String getBaseURL() {
        return generateURL(request, request.getContextPath());
    }

    private String generateURL(HttpServletRequest request, String contextPath) {
        final Properties webProperties = InterMineContext.getWebProperties();
        String baseUrl = webProperties.getProperty("webapp.baseurl");

        if (StringUtils.isEmpty(baseUrl)) {
            return getCurrentURL(request, contextPath);
        }

        if (request.getServerPort() != 80) {
            baseUrl += ":" + request.getServerPort();
        }
        String path = webProperties.getProperty("webapp.path");
        URL url = null;
        try {
            url = new URL(baseUrl + "/" + path);
        } catch (MalformedURLException e) {
            // whoops somethings gone terribly wrong. Use the URL
            return getCurrentURL(request, contextPath);
        }
        return url.toString();
    }

    // only use if they haven't set up baseURL
    private String getCurrentURL(HttpServletRequest request, String contextPath) {
        String port = "";
        if (request.getServerPort() != 80) {
            port = ":" + request.getServerPort();
        }
        String ret = request.getScheme() + "://" + request.getServerName() + port;
        if (contextPath.length() > 0) {
            ret += contextPath;
        }
        return ret;
    }
}
