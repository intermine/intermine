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

import org.intermine.webservice.client.services.FindTemplatesService;
import org.intermine.webservice.client.services.ListService;
import org.intermine.webservice.client.services.ModelService;
import org.intermine.webservice.client.services.QueryService;
import org.intermine.webservice.client.services.TemplateService;


/**
 * Class that should be used for creating services. You should never create instance of 
 * service class directly with constructors but always use this factory. If you use directly 
 * constructors then future versions of this client library can be incompatible with your code.    
 * 
 * @author Jakub Kulaviak
 *
 */
public class ServiceFactory 
{

    private String rootUrl;
    
    private String applicationName;
    
    /**
     * Constructor.
     * @param rootUrl base URL of all services, it is prefix common for all services. 
     * Example: http://www.flymine.org/service 
     * @param applicationName application name, information for server which application uses this 
     * service
     */
    public ServiceFactory(String rootUrl, String applicationName) {
        this.rootUrl = rootUrl;
        this.applicationName = applicationName;
    }

    /**
     * @return query service
     */
    public QueryService getQueryService() {
        return new QueryService(rootUrl, applicationName);
    }
    
    /**
     * @return template service
     */
    public TemplateService getTemplateService() {
        return new TemplateService(rootUrl, applicationName);
    }
    
    /**
     * @return list service
     */
    public ListService getListService() {
        return new ListService(rootUrl, applicationName);
    }
    
    /**
     * @return model service
     */
    public ModelService getModelService() {
        return new ModelService(rootUrl, applicationName);
    }    
    
    /**
     * @return model service
     */
    public FindTemplatesService getFindTemplatesService() {
        return new FindTemplatesService(rootUrl, applicationName);
    } 
    
    /**
     * Creates new service for general use. 
     * @param serviceRelativeUrl part of url specific for this service
     * Example: query/results
     * @param applicationName application name, information for server which application uses this 
     * service
     * @return created service
     */
    public Service getService(String serviceRelativeUrl, 
            String applicationName) {
        return new Service(rootUrl, serviceRelativeUrl, applicationName);
    }
}

