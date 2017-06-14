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

import java.io.IOException;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

/**
 * Very simple http client that downloads data.
 *
 * @author Jakub Kulaviak
 **/
public class HttpClient
{
    /**
     * @param url url of resource to be downloaded
     * @return downloaded data
     */
    public byte[] download(String url) {

        byte[] responseBody;

        // Create an instance of HttpClient.
        org.apache.commons.httpclient.HttpClient client =
            new org.apache.commons.httpclient.HttpClient();

        // Create a method instance.
        GetMethod method = new GetMethod(url);

        // Provide custom retry handler is necessary
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                new DefaultHttpMethodRetryHandler(3, false));

        try {
            // Execute the method.
            client.executeMethod(method);

            // Read the response body.
            responseBody = method.getResponseBody();

            // Deal with the response.
            // Use caution: ensure correct character encoding and is not binary
            // data
            return responseBody;
        } catch (HttpException e) {
            throw new RuntimeException("Fatal protocol violation.", e);
        } catch (IOException e) {
            throw new RuntimeException("Fatal transport error.", e);
        } finally {
            // Release the connection.
            method.releaseConnection();
        }
    }
}
