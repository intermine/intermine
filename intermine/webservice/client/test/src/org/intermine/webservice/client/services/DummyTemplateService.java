package org.intermine.webservice.client.services;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.junit.Assert.assertEquals;

import org.intermine.webservice.client.core.Request;
import org.intermine.webservice.client.results.XMLTableResult;

/**
 * Provides a dummy-fied TemplateService.
 *
 * @author Matthew Wakeling
 **/
public class DummyTemplateService extends TemplateService
{
    String fakeResponse = null;
    String expectedUrl = null;

    public DummyTemplateService(String rootUrl, String applicationName) {
        super(rootUrl, applicationName);
    }

    /**
     * Set a fake response, for testing purposes. Allows fake http responses to be inserted into
     * this object, so that testing does not require a real server to be set up.
     *
     * @param fakeResponses a String
     */
    public void setFakeResponse(String fakeResponse) {
        this.fakeResponse = fakeResponse;
    }

    public void setExpectedRequest(String url) {
        this.expectedUrl = url;
    }

    @Override
    protected XMLTableResult getResponseTable(Request request) {
        assureOutputFormatSpecified(request);
        if (!request.getEncodedUrl().equals(expectedUrl)) {
            assertEquals(expectedUrl, request.getUnencodedUrl());
        }
        return new XMLTableResult(fakeResponse);
    }
}

