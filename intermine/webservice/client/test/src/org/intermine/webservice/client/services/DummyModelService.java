package org.intermine.webservice.client.services;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Provides a dummy-fied ModelService.
 * 
 * @author Matthew Wakeling
 **/
public class DummyModelService extends ModelService
{
    String fakeResponse = null;

    public DummyModelService(String rootUrl, String applicationName) {
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

    protected String getModelXml() {
        return fakeResponse;
    }
}

