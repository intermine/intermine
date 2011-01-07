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
import org.apache.log4j.Logger;
import org.intermine.webservice.client.exceptions.ServiceException;
import org.intermine.webservice.client.util.HttpConnection;


/**
 * This class provides the base level common functionality required to access
 * any InterMine service. It is also designed to act as a base class that can be
 * customized for specific types of InterMine services. It encapsulates all protocol-level
 * interactions with the server.
 *
 * @author Jakub Kulaviak
 **/
public class Service
{

    private static final String VERSION_HEADER = "InterMine-Version";

    private static final String USER_AGENT_HEADER = "User-Agent";

    private static final String AUTHENTICATION_FIELD_NAME = "Authorization";

    private static Logger logger = Logger.getLogger(Service.class);

    protected URL url;

    private String rootUrl;

    private String applicationName;

    private int timeout;

    private String userName;

    private String password;

    /**
     * Constructor. {@link ServiceFactory} should be used always to create service and not this
     * constructor.
     * @param rootUrl base url of all services, it is prefix common for all services,
     *      Example: http://www.flymine.org/service
     * @param serviceRelativeUrl part of url specific for this service
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
            this.url = new URL(rootUrl + serviceRelativeUrl);
        } catch (MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    /**
     * Sets the username and password for all requests.
     *
     * @param userName a username
     * @param password a password
     */
    public void setAuthentication(String userName, String password) {
        if (userName == null || password == null || userName.length() == 0 
                || password.length() == 0) {
            throw new ServiceException("User name or password or both are empty.");
        }
        this.userName = userName;
        this.password = password;
    }

    /**
     * Open connection and returns connection.
     * @param request request
     * @return created connection
     */
    public HttpConnection executeRequest(Request request) {
        assureOutputFormatSpecified(request);
        String url = request.getUrl(true);
        request.setHeader(VERSION_HEADER, getVersion().toString());
        request.setHeader(USER_AGENT_HEADER, getApplicationName() + " " 
                + "JavaLibrary/" + getVersion().toString());
        setAuthenticationHeader(request);
        HttpConnection connection = new HttpConnection(request);
        connection.setTimeout(timeout);
        logger.debug("Executing request: " + url);
        connection.connect();
        return connection;
    }

    private void setAuthenticationHeader(Request request) {
        if (userName != null && password != null) {
            String authValue = userName + ":" + password;
            String encodedValue = new String(Base64.encodeBase64(authValue.getBytes()));
            request.setHeader(AUTHENTICATION_FIELD_NAME, encodedValue);            
        }
    }

    private void assureOutputFormatSpecified(Request request) {
        if (request.getParameter("format") == null 
                && getFormatValue(request.getContentType()) != null) {
            request.setParameter("format", getFormatValue(request.getContentType()));
        }
    }

    private String getFormatValue(ContentType contentType) {
        if (contentType == ContentType.TEXT_TAB) {
            return "tab";
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
     * Example: http://www.flymine.org/query/results
     * @return URL
     */
    public String getUrl() {
        if (url != null) {
            return url.toString();
        } else {
            return null;
        }
    }

    /**
     * Returns services root url.
     * Example: http://www.flymine.org/service 
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
     * @return service version
     */
    public Version getVersion() {
        return new Version("1.0");
    }

    /**
     * @return application name
     */
    public String getApplicationName() {
        return applicationName;
    }
}
