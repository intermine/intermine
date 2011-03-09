package org.intermine.webservice.client.services;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.webservice.client.core.TabTableResult;

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

    protected TabTableResult getResponseTable(TemplateRequest request) {
        if (!request.getUrl(true).equals(expectedUrl)) {
            throw new IllegalArgumentException("Expected URL \"" + expectedUrl + "\" does not match got URL \"" + request.getUrl(true) + "\"");
        }
        return new TabTableResult(fakeResponse);
    }
}

