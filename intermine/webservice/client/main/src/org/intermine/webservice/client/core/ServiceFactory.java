package org.intermine.webservice.client.core;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.webservice.client.services.AvailableTemplatesService;
import org.intermine.webservice.client.services.ListService;
import org.intermine.webservice.client.services.ModelService;
import org.intermine.webservice.client.services.QueryService;
import org.intermine.webservice.client.services.TemplateService;

/**
 * The Class that should be used for creating services. You should never create instances of
 * service classes directly with their constructors - it is simpler to use this factory.
 * This factory deals with constructing services with the correct relative paths.
 *
 * Usage:
 * <pre>
 *   ServiceFactory serviceFactory = new ServiceFactory("http://www.flymine.org/query/service", "MyApp")
 *   QueryService queryService = serviceFactory.getQueryService()
 *
 *      ...
 * </pre>
 *
 * @author Jakub Kulaviak
 *
 */
public class ServiceFactory
{

    private final String rootUrl;
    private String applicationName = "InterMine-WS-Client-Java-0.96";

    /**
     * Construct a service factory with a default application name. (defaults to
     * "InterMine-WS-Client-Java-$VERSION")
     * @param rootUrl the base URL for all services, it is the prefix common to all services.
     *      Example: http://www.flymine.org/query/service
     */
    public ServiceFactory(String rootUrl) {
        this.rootUrl = rootUrl;
    }

    /**
     * Construct a factory for gaining access to specific resources. Allows you to set 
     * the root url and the User-Agent identifier.
     * @param rootUrl the base URL for all services, it is the prefix common to all services.
     *      Example: http://www.flymine.org/query/service
     * @param applicationName application name, information to identify your application to the
     *      server
     */
    public ServiceFactory(String rootUrl, String applicationName) {
        this.rootUrl = rootUrl;
        this.applicationName = applicationName;
    }

    /**
     * Return a new QueryService for getting query results from.
     * @return query service
     */
    public QueryService getQueryService() {
        return new QueryService(rootUrl, applicationName);
    }

    /**
     * Return a new QueryService for getting query results from.
     * @return template service
     */
    public TemplateService getTemplateService() {
        return new TemplateService(rootUrl, applicationName);
    }

    /**
     * Return a new ListService for getting getting list information from.
     * @return list service
     */
    public ListService getListService() {
        return new ListService(rootUrl, applicationName);
    }

    /**
     * Return a new ModelService for retrieving the data model.
     * @return model service
     */
    public ModelService getModelService() {
        return new ModelService(rootUrl, applicationName);
    }

    /**
     * Return a new AvailableTemplatesService for getting lists of templates from.
     * @return available templates service
     */
    public AvailableTemplatesService getAvailableTemplatesService() {
        return new AvailableTemplatesService(rootUrl, applicationName);
    }

    /**
     * Creates a new service for general use.
     *
     * You very likely won't want to use this, unless you know exactly what resource you
     * are requesting.
     *
     * @param serviceRelativeUrl the part of the URL specific to this service
     *      Example: query/results
     * @param applicationName application name, information for server which application uses this
     *      service
     * @return the created service
     */
    public Service getService(String serviceRelativeUrl,
            String applicationName) {
        return new Service(rootUrl, serviceRelativeUrl, applicationName);
    }
}

