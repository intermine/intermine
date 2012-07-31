package org.intermine.webservice.client.core;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.webservice.client.results.Page;

/**
 * Client request interface. Client creates the request using following
 * methods and sends it to the server.
 * @author Jakub Kulaviak
 */
public interface Request
{

    public enum RequestType {
        /**
         * GET type.
         */
        GET,
        /**
         * POST type.
         */
        POST,
        /**
         * DELETE type
         */
        DELETE
    }

    /**
     * Adds parameter.
     * @param name parameter name
     * @param value parameter value
     */
    void addParameter(String name, String value);

    /**
     * @param name parameter name
     * @return parameter values
     */
    List<String> getParameterValues(String name);

    /**
     * Use only if you are sure, that the parameter has
     * only one value else use getParameterValues method.
     * @param name parameter name
     * @return parameter value
     */
    String getParameter(String name);

    /**
     * @return names of all parameters
     */
    Set<String> getParameterNames();

    /**
     * Sets parameter
     * @param name parameter name
     * @param value parameter value
     */
    void setParameter(String name, String value);

    /** Set the authentication token for the request
     * @param token the Authentication token for this request.
     */
    void setAuthToken(String token);

    /**
     * @return request type
     * @see RequestType
     */
    RequestType getType();

    /**
     * @param type request type
     * @see RequestType
     */
    void setType(RequestType type);

    /**
     * Returns service URL. Service URL is the URL of service without service parameters.
     * Example: http://www.flymine.org/service/query/results
     * @return URL as a string
     */
    String getServiceUrl();

    /**
     * Sets service URL.
     * @param url URL as a string
     * @see #setType(org.intermine.webservice.client.core.Request.RequestType)
     */
    void setServiceUrl(String url);

    /**
     * Sets whole request URL. Must not be URL encoded.
     * @param url URL
     */
    void  setUrl(String url);

    /**
     * @see <a href="http://www.ietf.org/rfc/rfc1738.txt">URL encoding specification</a>
     * @return the URL-encoded URL.
     */
    String getEncodedUrl();

    /**
     * @return Get the URL as an un-encoded (ie. human-readable) string.
     */
    String getUnencodedUrl();

    /**
     * @return content type
     */
    ContentType getContentType();

    /**
     * @param contentType content type
     */
    void setContentType(ContentType contentType);

    /**
     * Returns all parameters as an unmodifiable map.
     * @return map
     */
    Map<String, List<String>> getParameterMap();

    /**
     * Sets a request header.
     *
     * @param name the header name
     * @param value the header value
     */
    void setHeader(String name, String value);

    /**
     * Returns the header value.
     *
     * @param name the header name
     * @return the header value
     */
    String getHeader(String name);

    /**
     * Returns request headers.
     * @return headers
     */
    Map<String, String> getHeaders();

    /**
     * Set the page for this request.
     *
     * The page delimits which subsection of the results you
     * wish to receive.
     *
     * @param page The page
     */
    void setPage(Page page);

}

