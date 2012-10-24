package org.intermine.web.logic.export;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletResponse;


/**
 * Response util that sets content type and header for various formats and has
 * util methods for setting headers controlling cache.
 * @author Jakub Kulaviak
 **/
public final class ResponseUtil
{
    private ResponseUtil() {
        // do nothing
    }

    /**
     * Sets response header and content type for tab separated
     * values output.
     * @param response response
     * @param fileName file name of downloaded file
     */
    public static void setTabHeader(HttpServletResponse response, String fileName) {
        setNoCache(response);
        setTabContentType(response);
        setFileName(response, fileName);
    }

    /**
     * Sets response header and content type for comma separated
     * values output.
     * @param response response
     * @param fileName file name of downloaded file
     */
    public static void setCSVHeader(HttpServletResponse response, String fileName) {
        setNoCache(response);
        setCSVContentType(response);
        setFileName(response, fileName);
    }

    /**
     * Sets response header and content type for XML output.
     * @param response response
     * @param fileName file name of downloaded file
     */
    public static void setXMLHeader(HttpServletResponse response, String fileName) {
        setNoCache(response);
        setXMLContentType(response);
        setFileName(response, fileName);
    }


    /**
     * Sets response header and content type for plain text output.
     * @param response response
     * @param fileName file name of downloaded file
     */
    public static void setPlainTextHeader(HttpServletResponse response, String fileName) {
        setNoCache(response);
        setPlainTextContentType(response);
        setFileName(response, fileName);
    }
    
    /**
     * Sets response header and content type for gzipped output.
     *
     * @param response response
     * @param fileName file name of downloaded file
     */
    public static void setGzippedHeader(HttpServletResponse response, String fileName) {
        setNoCache(response);
        setGzippedContentType(response);
        setFileName(response, fileName);
    }

    /**
     * Sets the response header and content type for json output
     * @param response The response we are sending into the world
     * @param filename The filename this response should have
     */
    public static void setJSONHeader(HttpServletResponse response,
            String filename) {
        setJSONContentType(response);
        setFileName(response, filename);
        setNoCache(response);
    }

    public static void setJSONSchemaHeader(HttpServletResponse response,
            String filename) {
        setJSONSchemaContentType(response);
        setFileName(response, filename);
        setNoCache(response);
    }

    /**
     * Sets the response header and content type for jsonp output
     * @param response Our response to this request
     * @param filename The name this response should have
     */
    public static void setJSONPHeader(HttpServletResponse response,
            String filename) {
        setJSONPContentType(response);
        setFileName(response, filename);
        setNoCache(response);
    }

    /**
     * Sets response header and content type for a custom content type.
     * @param response response
     * @param fileName file name of downloaded file
     * @param contentType the content type to use
     */
    public static void setCustomTypeHeader(HttpServletResponse response, String fileName,
            String contentType) {
        setNoCache(response);
        setCustomContentType(response, contentType);
        setFileName(response, fileName);
    }

    /**
     * Sets that the result must not be cached. Old implementation was set
     * Cache-Control to no-cache,no-store,max-age=0. But this caused problems
     * in IE. File couldn't be opened directly.
     * @param response response
     */
    public static void setNoCache(HttpServletResponse response) {
        // http://www.phord.com/experiment/cache/
        // http://support.microsoft.com/kb/243717
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "must-revalidate, max-age=0");
    }

    /**
     * Sets enforced no-cache headers to completely disable cache for this response.
     * Page is reloaded always, for example when the user uses Go Back button.
     * @param response response
     */
    public static void setNoCacheEnforced(HttpServletResponse response) {
        // should work for firefox and IE to refresh always the page when back button is pressed
        // http://forums.mozillazine.org/viewtopic.php?f=25&t=673135&start=30
        response.setHeader("Cache-Control", "max-age=0, must-revalidate, no-store, no-cache");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "Wed, 11 Jan 1984 05:00:00 GMT");
    }

    /**
     * Sets tab separated values content type.
     * @param response response
     */
    public static void setTabContentType(HttpServletResponse response) {
        response.setContentType("text/tab-separated-values");
        response.setCharacterEncoding("UTF-8");
    }

    /**
     * Sets comma separated values content type.
     * @param response response
     */
    public static void setCSVContentType(HttpServletResponse response) {
        response.setContentType("text/comma-separated-values");
        response.setCharacterEncoding("UTF-8");
    }

    /**
     * Sets plain text content type.
     * @param response response
     */
    public static void setPlainTextContentType(HttpServletResponse response) {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
    }

    /**
     * Sets XML content type.
     * @param response response
     */
    public static void setXMLContentType(HttpServletResponse  response) {
        response.setContentType("text/xml");
        response.setCharacterEncoding("UTF-8");
    }

    /**
     * Sets HTML content type.
     * @param response response
     */
    public static void setHTMLContentType(HttpServletResponse response) {
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
    }

    /**
     * Sets gzip content type ("application/octet-stream")
     * @param response response
     */
    public static void setGzippedContentType(HttpServletResponse response) {
        response.setContentType("application/octet-stream");
    }

    /**
     * Sets content type to the parameter specified
     * @param response response
     * @param contentType custom MIME type to set as content type specified
     */
    public static void setCustomContentType(HttpServletResponse response, String contentType) {
        response.setContentType(contentType);
    }
    /**
     * Sets the content disposition filename.
     *
     * @param response response
     * @param fileName the name of the downloaded file
     */
    public static void setFileName(HttpServletResponse response, String fileName) {
        response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
    }

    /**
     * Sets the content type to "application/json"
     * @param response The response we are sending out into the world
     */
    public static void setJSONContentType(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
    }

    /**
     * Sets the content type to "application/schema+json"
     * @param response The response we are sending out into the world
     */
    public static void setJSONSchemaContentType(HttpServletResponse response) {
        response.setContentType("application/schema+json");
        response.setCharacterEncoding("UTF-8");
    }

    /**
     * Sets the content type to "text/javascript"
     * @param response The response we are sending out into the world
     */
    public static void setJSONPContentType(HttpServletResponse response) {
        response.setContentType("text/javascript");
        response.setCharacterEncoding("UTF-8");
    }
}

