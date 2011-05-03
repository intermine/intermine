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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.intermine.webservice.client.core.ContentType;
import org.intermine.webservice.client.core.Request;
import org.intermine.webservice.client.core.Request.RequestType;
import org.intermine.webservice.client.core.RequestImpl;
import org.intermine.webservice.client.core.Service;
import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.exceptions.ServiceException;
import org.intermine.webservice.client.util.HttpConnection;


/**
 * The AvailableTemplatesService is a service for fetching the names of template queries
 * available in an InterMine instance
 *
 * @author Richard Smith
 **/
public class AvailableTemplatesService extends Service
{

    private static final String SERVICE_RELATIVE_URL = "templates";

    /**
     * Use {@link ServiceFactory} instead of constructor for creating this service .
     * @param rootUrl root URL
     * @param applicationName application name
     */
    public AvailableTemplatesService(String rootUrl, String applicationName) {
        super(rootUrl, SERVICE_RELATIVE_URL, applicationName);
    }

    /**
     * Returns model used by InterMine instance to which the service
     * is connected.
     * @return model
     */
    public List<String> getTemplateNames() {
        Request request = new RequestImpl(RequestType.GET, getUrl(),
                ContentType.TEXT_PLAIN);
        HttpConnection connection = executeRequest(request);

        List<String> ret = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection
                .getResponseBodyAsStream()));

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                ret.add(line);
            }
        } catch (IOException e) {
            throw new ServiceException("Reading from response stream failed", e);
        } finally {
            connection.close();
        }
        return ret;
    }
}
