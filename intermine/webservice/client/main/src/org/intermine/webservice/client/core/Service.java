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

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.codec.binary.Base64;
import org.intermine.webservice.client.exceptions.ServiceException;
import org.intermine.webservice.client.util.HttpConnection;


/**
 * This class provides the base level common functionality required to access
 * any InterMine service. It is also designed to act as a base class that can be
 * customised for specific types of InterMine services. It encapsulates all protocol-level
 * interactions with the server.
 *
 * @author Jakub Kulaviak
 **/
public class Service
{

    /** The version of this client library. **/
    public static final Version VERSION = new Version(3, 0, 0);

    private static final String VERSION_HEADER = "InterMine-Version";

    private static final String USER_AGENT_HEADER = "User-Agent";

    private static final String AUTHENTICATION_FIELD_NAME = "Authorization";

    protected URL resourceUrl;

    private String rootUrl;

    private String applicationName;

    private int timeout;

    private String userName;

    private String password;

    private String authToken;

    private ServiceFactory factory = null;

    /**
     * Get the ServiceFactory this service was constructed with.
     * @return The parent service-factory.
     */
    public ServiceFactory getFactory() {
        return factory;
    }

    /**
     * Set the ServiceFactory this service can use to access other services.
     * @param factory The parent service-factory.
     */
    public void setFactory(ServiceFactory factory) {
        this.factory = factory;
    }

    /**
     * Constructor. {@link ServiceFactory} should be used always to create services and not this
     * constructor.
     * @param rootUrl the base URL of all services, it is the common prefix for all services,
     *      Example: http://www.flymine.org/service
     * @param serviceRelativeUrl the part of the URL specific to this service
     *      Example: query/results
     * @param applicationName application name, information for server which application uses
     * this service
     */
    public Service(String rootUrl, String serviceRelativeUrl,
            String applicationName) {
        init(rootUrl, serviceRelativeUrl, applicationName);
    }

    private void init(String rootUrl, String serviceRelativeUrl,
            String applicationName) {
        this.rootUrl = rootUrl;
        this.applicationName = applicationName;
        if (!rootUrl.endsWith("/")) {
            rootUrl = rootUrl + "/";
        }
        try {
            this.resourceUrl = new URL(rootUrl + serviceRelativeUrl);
        } catch (MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Sets the user-name and password for all requests.
     *
     * @param userName a user-name
     * @param password a password
     *
     * @deprecated Use token based authentication instead ({@link #setAuthentication(String)}
     * as this method will cause your user name and password to be transmitted
     * insecurely across HTTP connections.
     */
    @Deprecated
    public void setAuthentication(String userName, String password) {
        if (userName == null || password == null || userName.length() == 0
                || password.length() == 0) {
            throw new ServiceException("User name or password or both are empty.");
        }
        this.userName = userName;
        this.password = password;
    }

    /**
     * Set the token to be used for authentication. This is the preferred mechanism for
     * authenticating requests to the web-service.
     * @param token The token to use for authentication.
     */
    public void setAuthentication(String token) {
        if (token == null) {
            throw new ServiceException("authorization token cannot be null.");
        }
        this.authToken = token;
    }

    /**
     * Open connection and returns connection.
     * @param request request
     * @return created connection
     */
    public HttpConnection executeRequest(Request request) {
        assureOutputFormatSpecified(request);
        request.setHeader(VERSION_HEADER, getVersion().toString());
        request.setHeader(USER_AGENT_HEADER, getApplicationName() + " "
                + "JavaLibrary/" + getVersion().toString());
        applyAuthentication(request);
        HttpConnection connection = new HttpConnection(request);
        connection.setTimeout(timeout);
        connection.connect();
        return connection;
    }

    private void applyAuthentication(Request request) {
        if (userName != null && password != null) {
            String authValue = userName + ":" + password;
            String encodedValue = new String(Base64.encodeBase64(authValue.getBytes()));
            request.setHeader(AUTHENTICATION_FIELD_NAME, encodedValue);
        } else if (authToken != null) {
            request.setAuthToken(authToken);
        }
    }

    /**
     * Add a format parameter to match the content-type if there is one.
     *
     * @param request The request to fool around with.
     */
    protected void assureOutputFormatSpecified(Request request) {
        // This is such a bad implementation of REST principles it makes my eyes bleed.
        if (request.getParameter("format") == null
                && getFormatValue(request.getContentType()) != null) {
            request.setParameter("format", getFormatValue(request.getContentType()));
        }
    }

    private String getFormatValue(ContentType contentType) {
        if (contentType == ContentType.TEXT_TAB) {
            return "tab";
        } else if (contentType == ContentType.APPLICATION_JSON_OBJ) {
            return "jsonobjects";
        } else if (contentType == ContentType.APPLICATION_JSON_ROW) {
            return "jsonrows";
        } else if (contentType == ContentType.TEXT_COUNT) {
            return "count";
        } else if (contentType == ContentType.APPLICATION_JSON) {
            return "json";
        } else if (contentType == ContentType.TEXT_XML) {
            return "xml";
        }
        return null;
    }

    /**
     * Sets connection timeout.
     * @param timeout timeout
     */
    public void setConnectionTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Returns service URL
     * Example: http://www.flymine.org/query/service/query/results
     * @return URL
     */
    public String getUrl() {
        if (resourceUrl != null) {
            return resourceUrl.toString();
        } else {
            return null;
        }
    }

    /**
     * Returns service's root URL.
     * Example: http://www.flymine.org/query/service
     * @return URL
     */
    public String getRootUrl() {
        return rootUrl;
    }

    /**
     * Creates GET request.
     * @param url URL of request
     * @param contentType required content type of response
     * @return created request
     */
    public Request createGetRequest(String url, ContentType contentType) {
        return new RequestImpl(Request.RequestType.GET, url, contentType);
    }

    /**
     * Creates POST request.
     * @param requestUrl URL of request
     * @param contentType required content type of response
     * @return created request
     */
    public Request createPostRequest(String requestUrl, ContentType contentType) {
        return new RequestImpl(Request.RequestType.POST, requestUrl, contentType);
    }

    /**
     * @return The client version
     */
    public Version getVersion() {
        return VERSION;
    }

    /**
     * @return application name
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * Performs the request and returns the result as a string.
     * @param request The Request object
     * @param retryCount The number of times to retry. If null, the default value of HttpConnection
     *        will be used.
     * @return a string containing the body of the response
     */
    protected String getStringResponse(Request request, Integer retryCount) {
        HttpConnection connection = executeRequest(request);
        if (retryCount != null) {
            connection.setRetryCount(retryCount.intValue());
        }
        String res = null;
        try {
            res = connection.getResponseBodyAsString().trim();
        } finally {
            connection.close();
        }
        return res;
    }

    /**
     * Performs the request and returns the result as a string.
     * @param request The Request object
     * @return a string containing the body of the response
     */
    protected String getStringResponse(Request request) {
        return getStringResponse(request, null);
    }

    /**
     * Performs the request and returns the result as an integer.
     * Suitable when the service returns a single integer number.
     * @param request The Request object
     * @return an integer.
     */
    protected int getIntResponse(Request request) {
        String body = getStringResponse(request);
        if (body.length() == 0) {
            throw new ServiceException("The server didn't return any results");
        }
        try {
            return Integer.parseInt(body);
        }  catch (NumberFormatException e) {
            throw new ServiceException(
                    "The server returned an invalid result. It is not a number: "
                    + body, e);
        }
    }

    /**
     * @return the server's API version.
     */
    public int getAPIVersion() {
        Request r = createGetRequest(getRootUrl() + "/version", ContentType.TEXT_PLAIN);
        return getIntResponse(r);
    }

    /**
     * @return the server's release.
     */
    public String getRelease() {
        Request r = createGetRequest(getRootUrl() + "/version/release", ContentType.TEXT_PLAIN);
        return getStringResponse(r, 0);
    }
}
