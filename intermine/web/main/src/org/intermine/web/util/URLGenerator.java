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

import javax.servlet.http.HttpServletRequest;

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
        String port = "";
        if (request.getServerPort() != 80) {
            port = ":" + request.getServerPort();
        }
        String ret = "http://" + request.getServerName() + port;
        if (contextPath.length() > 0) {
            ret += contextPath;
        }
        return ret;
    }
}
