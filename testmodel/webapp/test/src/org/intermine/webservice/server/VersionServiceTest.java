package org.intermine.webservice.server;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.intermine.web.logic.Constants;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class VersionServiceTest {

    private String baseurl;
    private HttpClient client = new HttpClient();
    private static int expectedVersion = Constants.WEB_SERVICE_VERSION;

    @Before
    public void setup() throws Exception {
        try {
            baseurl = TestUtil.getServiceBaseURL();
        } catch (Exception e) {
            // For testing in eclipse.
            baseurl = "http://localhost/intermine-test/service";
        }
    }

    @Test
    public void wsVersion() throws HttpException, IOException {
        HttpMethod get = new GetMethod(baseurl + "/version");
        try {
            int statusCode = client.executeMethod(get);
            assertEquals("Request should be successful", 200, statusCode);
            assertEquals("Version should be " + expectedVersion,
                    "" + expectedVersion,
                    get.getResponseBodyAsString().trim());
        } finally {
            get.releaseConnection();
        }
    }

    @Test
    public void wsVersionJSON() throws Exception {
        HttpMethod get = new GetMethod(baseurl + "/version/json");
        try {
            int statusCode = client.executeMethod(get);
            assertEquals("Request should be successful", 200, statusCode);
            JSONObject resp = new JSONObject(get.getResponseBodyAsString());
            assertEquals("Version should be " + expectedVersion,
                    expectedVersion, resp.getInt("version"));
        } finally {
            get.releaseConnection();
        }
    }

    @Test
    public void wsVersionJSONP() throws Exception {
        HttpMethod get = new GetMethod(baseurl + "/version?callback=foo");
        try {
            int statusCode = client.executeMethod(get);
            assertEquals("Request should be successful", 200, statusCode);
            String body = get.getResponseBodyAsString();
            assertTrue("JSONP responses have the callback", body.startsWith("foo"));
            String json = body.substring(body.indexOf("foo(") + 4, body.length() - 1);
            JSONObject resp = new JSONObject(json);
            assertEquals("Version should be " + expectedVersion,
                    expectedVersion, resp.getInt("version"));
        } finally {
            get.releaseConnection();
        }
    }
}
