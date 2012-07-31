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

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.intermine.metadata.Model;
import org.intermine.modelproduction.xml.InterMineModelParser;
import org.intermine.webservice.client.core.ContentType;
import org.intermine.webservice.client.core.Request;
import org.intermine.webservice.client.core.Request.RequestType;
import org.intermine.webservice.client.core.RequestImpl;
import org.intermine.webservice.client.core.Service;
import org.intermine.webservice.client.exceptions.ServiceException;
import org.intermine.webservice.client.util.HttpConnection;


/**
 * This class represents a connection to the RESTful resource on an InterMine server which
 * provides information about the service's data model. This is a structure which is serialised
 * and transmitted in XML, and contains information about the tables within the database in
 * a highly abstract manner. The data model is required for constructing queries with, in order
 * to validate them appropriately.
 *
 * @see org.intermine.metadata.Model
 * @see org.intermine.pathquery.PathQuery
 * @see org.intermine.webservice.client.services.QueryService
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
     * Please do not instantiate this class yourself directly - instead use the
     * {@link ServiceFactory} - this will ensure maintainability of your code.
     *
     * @param rootUrl root URL
     * @param applicationName application name
     */
    public ModelService(String rootUrl, String applicationName) {
        super(rootUrl, SERVICE_RELATIVE_URL, applicationName);
    }

    /**
     * Returns the model used by the InterMine instance which the service is connected to.
     *
     * @return model
     */
    public Model getModel() {
        String key = getRootUrl();
        Model model = models.get(key);
        if (model == null) {
            model = fetchModel();
            // definitions of classes are not available in the client
            // and you need to tell the model
            // so it won't do checks when constructing path query
            model.setGeneratedClassesAvailable(false);
            models.put(key, model);
            // This is necessary so that unmarshalling of queries can occur.
            Model.addModel(model.getName(), model);
        }
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
     * An method used internally to fetch the XML for the model from the server.
     *
     * @return the serialised representation of the data model.
     */
    protected String getModelXml() {
        Request request = new RequestImpl(RequestType.GET, getUrl(),
                ContentType.TEXT_PLAIN);
        HttpConnection connection = executeRequest(request);
        return connection.getResponseBodyAsString();
    }
}
