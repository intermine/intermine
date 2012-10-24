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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.methods.multipart.Part;

/**
 * A class for constructing multi-part form requests.
 * @author Alex Kalderimis
 *
 */
public class MultiPartRequest extends RequestImpl
{

    private List<Part> parts = new ArrayList<Part>();

    /**
     * Construct a new request to the given URL.
     * @param url The resource to POST to.
     */
    public MultiPartRequest(String url) {
        super(RequestType.POST, url, ContentType.MULTI_PART_FORM);
    }

    /**
     * @return The parts of this request.
     */
    public List<Part> getParts() {
        return parts;
    }
}
