package org.intermine.webservice.client.core;

/*
 * Copyright (C) 2002-2011 FlyMine
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
        POST
    }

    /**
     * Adds parameter.
     * @param name parameter name
     * @param value parameter value
     */
    public void addParameter(String name, String value);

    /**
     * @param name parameter name
     * @return parameter values
     */
    public List<String> getParameterValues(String name);

    /**
     * Use only if you are sure, that the parameter has
     * only one value else use getParameterValues method.
     * @param name parameter name
     * @return parameter value
     */
    public String getParameter(String name);

    /**
     * @return names of all parameters
     */
    public Set<String> getParameterNames();

    /**
     * Sets parameter
     * @param name parameter name
     * @param value parameter value
     */
    public void setParameter(String name, String value);

    /**
     * @return request type
     * @see RequestType
     */
    public RequestType getType();

    /**
     * @param type request type
     * @see RequestType
     */
    public void setType(RequestType type);

    /**
     * Returns service url. Service url is the url of service without service parameters.
     * Example: http://www.flymine.org/service/query/results
     * @return url
     */
    public String getServiceUrl();

    /**
     * Sets service URL.
     * @param url url
     * @see #setType(org.intermine.webservice.client.core.Request.RequestType)
     */
    public void setServiceUrl(String url);

    /**
     * Sets whole request URL. Must not be URL encoded.
     * @param url URL
     */
    public void  setUrl(String url);

    /**
     *
     * @param encode true if returned string should be url encoded
     * @see <a href="http://www.ietf.org/rfc/rfc1738.txt">URL encoding specification</a>
     * @return url
     */
    public String getUrl(boolean encode);

    /**
     * @return content type
     */
    public ContentType getContentType();

    /**
     * @param contentType content type
     */
    public void setContentType(ContentType contentType);

    /**
     * Returns all parameters as a modifiable map. You can
     * modify parameters manipulating directly with this map.
     * @return map
     */
    public Map<String, List<String>> getParameterMap();

    /**
     * Sets a request header.
     *
     * @param name the header name
     * @param value the header value
     */
    public void setHeader(String name, String value);

    /**
     * Returns the header value.
     *
     * @param name the header name
     * @return the header value
     */
    public String getHeader(String name);

    /**
     * Returns request headers.
     * @return headers
     */
    public Map<String, String> getHeaders();

}

