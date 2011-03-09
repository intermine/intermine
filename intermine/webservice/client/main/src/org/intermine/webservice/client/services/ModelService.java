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

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.intermine.metadata.Model;
import org.intermine.modelproduction.xml.InterMineModelParser;
import org.intermine.webservice.client.core.ContentType;
import org.intermine.webservice.client.core.Request;
import org.intermine.webservice.client.core.RequestImpl;
import org.intermine.webservice.client.core.Service;
import org.intermine.webservice.client.core.Request.RequestType;
import org.intermine.webservice.client.exceptions.ServiceException;
import org.intermine.webservice.client.util.HttpConnection;


/**
 * The ModelService is service returning model used by InterMine instance.
 *
 * @author Jakub Kulaviak
 **/
public class ModelService extends Service
{

    private static final String SERVICE_RELATIVE_URL = "model";

    // static map of models enables to cache already loaded models from
    // different services
    // map: service_root_url -> models
    private static Map<String, Model> models = new HashMap<String, Model>();

    /**
     * Use {@link ServiceFactory} instead of constructor for creating this service .
     * @param rootUrl root URL
     * @param applicationName application name
     */
    public ModelService(String rootUrl, String applicationName) {
        super(rootUrl, SERVICE_RELATIVE_URL, applicationName);
    }

    /**
     * Returns model used by InterMine instance to which the service
     * is connected.
     * @return model
     */
    public Model getModel() {
        String key = getRootUrl();
        Model model = models.get(key);
        if (model == null) {
            model = fetchModel();
            models.put(key, model);
        }
        // definitions of classes are not available in the client and you need to tell it to model
        // so it won't do checks when constructing path query
        model.setGeneratedClassesAvailable(false);
        return model;
    }

    private Model fetchModel() {
        String modelXml = getModelXml();
        Model model = null;
        try {
            model = new InterMineModelParser().process(new StringReader(modelXml));
        } catch (Exception e) {
            throw new ServiceException("Error occured during parsing model XML", e);
        }
        return model;
    }

    /**
     * Fetches the xml for the model from the server.
     *
     * @return the model in XML format
     */
    protected String getModelXml() {
        Request request = new RequestImpl(RequestType.GET, getUrl(),
                ContentType.TEXT_PLAIN);
        HttpConnection connection = executeRequest(request);
        return connection.getResponseBodyAsString();
    }
}
