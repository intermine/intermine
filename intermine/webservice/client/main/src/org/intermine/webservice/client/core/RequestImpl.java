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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.webservice.client.exceptions.ServiceException;
import org.intermine.webservice.client.results.Page;
import org.intermine.webservice.client.util.URLParser;


/**
 * Implementation of Request interface.
 * @author Jakub Kulaviak
 **/
public class RequestImpl implements Request
{

    protected static final String FORMAT_PARAMETER_JSON_OBJ = "jsonobjects";
    protected static final String FORMAT_PARAMETER_JSON_ROWS = "jsonrows";
    protected static final String FORMAT_PARAMETER_XML = "xml";
    protected static final String FORMAT_PARAMETER_COUNT = "count";

    private RequestType type;

    private String serviceUrl;

    private ContentType contentType;

    private Map<String, List<String>> parameters = new HashMap<String, List<String>>();

    private final Map<String, String> headers = new HashMap<String, String>();

    /**
     * Constructor.

    public RequestImpl() {
    }*/

    /**
     * Constructor.
     * @param type type
     * @param url URL
     * @param contentType content type
     */
    public RequestImpl(RequestType type, String url, ContentType contentType) {
        this.type = type;
        setUrl(url);
        this.contentType = contentType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addParameter(String name, String value) {
        List<String> values = getParameterValues(name);
        if (values == null) {
            values = new ArrayList<String>();
            parameters.put(name, values);
        }
        values.add(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getParameterValues(String name) {
        return parameters.get(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getParameter(String name) {
        List<String>  pars = getParameterValues(name);
        if (pars != null && pars.size() > 0) {
            return pars.get(0);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getParameterNames() {
        return parameters.keySet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParameter(String name, String value) {
        List<String> values = parameters.get(name);
        if (values == null) {
            values = new ArrayList<String>();
            parameters.put(name, values);
        }
        values.clear();
        values.add(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RequestType getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setType(RequestType type) {
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServiceUrl() {
        return serviceUrl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setServiceUrl(String url) {
        this.serviceUrl = url;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUrl(String url) {
        try {
            this.serviceUrl = URLParser.parseServiceUrl(url);
            this.parameters = URLParser.parseParameterMap(url);
        } catch (MalformedURLException e) {
            throw new ServiceException("Invalid url: " + url, e);
        }
    }

    /**
     * Sets the maximum number of rows returned.
     *
     * @param maxCount an integer number of rows, where outer joins count as multiple rows
     */
    public void setMaxCount(int maxCount) {
        setParameter("size", maxCount + "");
    }

    /**
     * Set the start parameter.
     * @param start The index of the first result to include.
     */
    public void setStart(int start) {
        setParameter("start", start + "");
    }

    /**
     * Set the format for the request.
     * @param format The format of the request.
     */
    public void setFormat(String format) {
        setParameter("format", format);
    }

    /**
     * Set the format as JSON-Object format.
     */
    public void setJSONFormat() {
        setFormat(FORMAT_PARAMETER_JSON_OBJ);
    }

    /**
     * Set the format as JSON-Rows format.
     */
    public void setJSONRowsFormat() {
        setFormat(FORMAT_PARAMETER_JSON_ROWS);
    }

    /**
     * Set the format as XML format.
     */
    public void setXMLFormat() {
        setFormat(FORMAT_PARAMETER_XML);
    }

    /**
     * Set the format as count format.
     */
    public void setCountFormat() {
        setFormat(FORMAT_PARAMETER_COUNT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ContentType getContentType() {
        return contentType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, List<String>> getParameterMap() {
        Map<String, List<String>> params = new HashMap<String, List<String>>(parameters);
        if (authToken != null) {
            params.put("token", Collections.singletonList(authToken));
        }
        return Collections.unmodifiableMap(params);
    }

    @Override
    public String getEncodedUrl() {
        return getUrl(true);
    }

    @Override
    public String getUnencodedUrl() {
        return getUrl(false);
    }

    /**
     * {@inheritDoc}
     */
    private String getUrl(boolean encode) {
        StringBuilder sb = new StringBuilder();
        sb.append(serviceUrl);
        String separator = "?";
        Map<String, List<String>> params = getParameterMap();
        for (String parName : params.keySet()) {
            for (String value : params.get(parName)) {
                sb.append(separator);
                if ("?".equals(separator)) {
                    separator = "&";
                }
                sb.append(parName);
                if (value.length() > 0) {
                    sb.append("=");
                    sb.append(format(value, encode));
                }
            }
        }
        return sb.toString();
    }

    private String format(String str, boolean encode) {
        if (encode) {
            try {
                return URLEncoder.encode(str, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new ServiceException("URL encoding failed for string " + str, e);
            }
        } else {
            return str;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHeader(String name) {
        return headers.get(name);
    }


    private String authToken = null;

    @Override
    public void setAuthToken(String token) {
        authToken = token;
    }

    @Override
    public String toString() {
        return type + " " + serviceUrl
                + ", params: " + parameters
                + ", authorization-token: " + authToken
                + ", content-type: " + contentType.toString()
                + ", headers: " + headers;
    }

    /**
     * Specify what section of the result set you wish to retrieve.
     * @param page the subsection of the result set you want.
     */
    public void setPage(Page page) {
        setStart(page.getStart());
        if (page.getSize() != null) {
            setMaxCount(page.getSize());
        }
    }
}
