package org.intermine.webservice.client.util;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.intermine.webservice.client.core.Request;
import org.intermine.webservice.client.core.Request.RequestType;
import org.intermine.webservice.client.exceptions.BadRequestException;
import org.intermine.webservice.client.exceptions.InternalErrorException;
import org.intermine.webservice.client.exceptions.NotConnectedException;
import org.intermine.webservice.client.exceptions.NotImplementedException;
import org.intermine.webservice.client.exceptions.ResourceNotFoundException;
import org.intermine.webservice.client.exceptions.ServiceException;
import org.intermine.webservice.client.exceptions.ServiceForbiddenException;
import org.intermine.webservice.client.exceptions.ServiceUnavailableException;

/**
 * The HttpConnection is class wrapping implementation details of http connection and the
 * implementation can change easily.
 *
 * @author Jakub Kulaviak
 **/
public class HttpConnection
{

    private Request request;

    HttpMethodBase executedMethod;

    private int timeout;

    private boolean opened = false;

    /**
     * @param request client request
     */
    public HttpConnection(Request request) {
        this.request = request;
    }

    /**
     * @return response stream
     */
    public InputStream getResponseStream() {
        connect();
        try {
            return executedMethod.getResponseBodyAsStream();
        } catch (IOException e) {
            throw new RuntimeException("Fatal transport error.", e);
        }
    }

    /**
     * Opens connection.
     */
    public void connect() {
        executeMethod();
        opened = true;
    }

    /**
     * @param name header name
     * @return response header
     */
    public String getResponseHeader(String name) {
        if (executedMethod == null) {
            throwNotConnectedException();
        }
        return executedMethod.getResponseHeader(name).getValue();
    }

    private void throwNotConnectedException() {
        throw new NotConnectedException();
    }

    /**
     * Closes connection.
     */
    public void close() {
        if (executedMethod != null) {
            executedMethod.releaseConnection();
            opened = false;
        }
    }

    private void executeMethod() {
        HttpClient client = new HttpClient();
        client.getParams().setConnectionManagerTimeout(timeout);
        String url = null;
        if (request.getType() == RequestType.GET) {
            executedMethod = new GetMethod(request.getUrl(true));
            url = request.getUrl(true);
        } else {
            PostMethod postMethod = new PostMethod(request.getServiceUrl());
            setPostMethodParameters(postMethod, request.getParameterMap());
            executedMethod = postMethod;
            url = request.getServiceUrl();
        }
        // Provide custom retry handler is necessary
        executedMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                new DefaultHttpMethodRetryHandler(3, false));
        for (String name : request.getHeaders().keySet()) {
            executedMethod.setRequestHeader(name, request.getHeader(name));
        }
        try {
            // Execute the method.
            client.executeMethod(executedMethod);
            checkResponse(url);
        } catch (HttpException e) {
            throw new RuntimeException("Fatal protocol violation.", e);
        } catch (IOException e) {
            throw new RuntimeException("Fatal transport error connecting to " + url, e);
        }
    }

    private void setPostMethodParameters(PostMethod postMethod,
            Map<String, List<String>> parameterMap) {
        for (String name : parameterMap.keySet()) {
            for (String value : parameterMap.get(name)) {
                postMethod.addParameter(name, value);
            }
        }
    }

    /**
     * Sets timeout.
     * @param timeout timeout in milliseconds
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * @return response code.
     */
    public int getResponseCode() {
        if (executedMethod == null) {
            throwNotConnectedException();
        }
        return executedMethod.getStatusCode();
    }

    /**
     *
     * @return true if connection is opened else false
     */
    public boolean isOpened() {
        return opened;
    }

    /**
     * Called to check the response and generate an appropriate exception (on failure).
     * If the connection is not opened then it is opened and response checked.
     *
     * @param url a URL to quote in any error messages
     * @throws ServiceException when an error happens
     */
    protected void checkResponse(String url) {
        if (executedMethod.getStatusCode() >= 300) {
            try {
                handleErrorResponse();
            } catch (ServiceException e) {
                throw new ServiceException("Error while accessing " + url, e);
            } catch (IOException e) {
                throw new ServiceException("Error while accessing " + url, e);
            }
        }
    }

    /**
     * Handles an error response received while executing a service request.
     * Throws a {@link ServiceException} or one of its subclasses, depending on
     * the failure conditions.
     *
     * @throws ServiceException exception describing the failure.
     * @throws IOException error reading the error response from the
     *         service.
     */
    protected void handleErrorResponse() throws IOException {

        switch (executedMethod.getStatusCode()) {

            case HttpURLConnection.HTTP_NOT_FOUND:
                throw new ResourceNotFoundException(this);

            case HttpURLConnection.HTTP_BAD_REQUEST:
                throw new BadRequestException(this);

            case HttpURLConnection.HTTP_FORBIDDEN:
                throw new ServiceForbiddenException(this);

            case HttpURLConnection.HTTP_NOT_IMPLEMENTED:
                throw new NotImplementedException(this);

            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                throw new InternalErrorException(this);

            case HttpURLConnection.HTTP_UNAVAILABLE:
                throw new ServiceUnavailableException(this);

            default:
                throw new ServiceException(this);
        }
    }

    /**
     * @return the response body body as the string
     */
    public String getResponseBodyAsString() {
        if (executedMethod == null) {
            throwNotConnectedException();
        }
        try {
            return executedMethod.getResponseBodyAsString();
        } catch (IOException e) {
            throw new ServiceException(e);
        }
    }

    /**
     * @return the response body as the stream
     */
    public InputStream getResponseBodyAsStream() {
        if (executedMethod == null) {
            throwNotConnectedException();
        }
        try {
            return executedMethod.getResponseBodyAsStream();
        } catch (IOException e) {
            throw new ServiceException(e);
        }
    }
}
