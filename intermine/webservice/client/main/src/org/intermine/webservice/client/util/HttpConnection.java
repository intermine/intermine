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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.ProxyHost;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.intermine.webservice.client.core.ContentType;
import org.intermine.webservice.client.core.MultiPartRequest;
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
import org.json.JSONException;
import org.json.JSONObject;

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

    private int retryCount = 3;

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

    private static void throwNotConnectedException() {
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
        setProxy(client);
        String url = request.getEncodedUrl();
        if (request.getType() == RequestType.GET) {
            executedMethod = new GetMethod(url);
        } else if (request.getType() == RequestType.DELETE) {
            executedMethod = new DeleteMethod(url);
        } else {
            PostMethod postMethod;
            if (request.getContentType() == ContentType.MULTI_PART_FORM) {
                postMethod = new PostMethod(url);
                setMultiPartPostEntity(postMethod, ((MultiPartRequest) request));
            } else {
                url = request.getServiceUrl();
                postMethod = new PostMethod(url);
                setPostMethodParameters(postMethod, request.getParameterMap());
            }
            executedMethod = postMethod;
        }
        // Provide custom retry handler is necessary
        executedMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                new DefaultHttpMethodRetryHandler(retryCount, false));
        for (String name : request.getHeaders().keySet()) {
            executedMethod.setRequestHeader(name, request.getHeader(name));
        }
        try {
            // Execute the method.
            client.executeMethod(executedMethod);
            checkResponse();
        } catch (HttpException e) {
            throw new RuntimeException("Fatal protocol violation.", e);
        } catch (IOException e) {
            throw new RuntimeException("Fatal transport error connecting to " + url, e);
        }
    }

    private static void setProxy(HttpClient client) {
        Properties systemProps = System.getProperties();
        if (systemProps.containsKey("http.proxyHost")) {
            String server = systemProps.getProperty("http.proxyHost");
            Integer port = Integer.valueOf(systemProps.getProperty("http.proxyPort", "-1"));
            ProxyHost ph = new ProxyHost(server, port);
            client.getHostConfiguration().setProxyHost(ph);
        }
    }

    private static void setMultiPartPostEntity(PostMethod postMethod, MultiPartRequest req) {
        List<Part> parts = req.getParts();
        if (!parts.isEmpty()) {
            RequestEntity entity = new MultipartRequestEntity(
                    req.getParts().toArray(new Part[1]), postMethod.getParams());
            postMethod.setRequestEntity(entity);
        }
    }

    private static void setPostMethodParameters(PostMethod postMethod,
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
     * Sets retry count.
     * @param times The number of times to flog a dead horse. (3 by default).
     */
    public void setRetryCount(int times) {
        this.retryCount = times;
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
     */
    protected void checkResponse() {
        if (executedMethod.getStatusCode() >= 300) {
            try {
                handleErrorResponse();
            } catch (ServiceException e) {
                throw new ServiceException("Error while accessing " + request, e);
            } catch (IOException e) {
                throw new ServiceException("Error while accessing " + request, e);
            }
        }
    }

    /**
     * Handles an error response received while executing a service request.
     * Throws a ServiceException or one of its subclasses, depending on
     * the failure conditions.
     *
     * @throws IOException error reading the error response from the
     *         service.
     */
    protected void handleErrorResponse() throws IOException {

        String message = executedMethod.getResponseBodyAsString();
        try {
            JSONObject jo = new JSONObject(message);
            message = jo.getString("error");
        } catch (JSONException e) {
            // Pass
        }

        switch (executedMethod.getStatusCode()) {

            case HttpURLConnection.HTTP_NOT_FOUND:
                throw new ResourceNotFoundException(this);

            case HttpURLConnection.HTTP_BAD_REQUEST:
                if (message != null) {
                    throw new BadRequestException(message);
                } else {
                    throw new BadRequestException(this);
                }
            case HttpURLConnection.HTTP_FORBIDDEN:
                if (message != null) {
                    throw new ServiceForbiddenException(message);
                } else {
                    throw new ServiceForbiddenException(this);
                }
            case HttpURLConnection.HTTP_NOT_IMPLEMENTED:
                throw new NotImplementedException(this);

            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                if (message != null) {
                    throw new InternalErrorException(message);
                } else {
                    throw new InternalErrorException(this);
                }
            case HttpURLConnection.HTTP_UNAVAILABLE:
                throw new ServiceUnavailableException(this);

            default:
                if (message != null) {
                    throw new ServiceException(message);
                } else {
                    throw new ServiceException(this);
                }
        }
    }

    /**
     * Return the response body, ensuring that the connection is closed
     * upon completion.
     * @return the response body body as the string
     */
    public String getResponseBodyAsString() {
        if (executedMethod == null) {
            throwNotConnectedException();
        }
        String res = null;
        try {
            res = executedMethod.getResponseBodyAsString();
        } catch (IOException e) {
            throw new ServiceException(e);
        } finally {
            executedMethod.releaseConnection();
        }
        return res;
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
