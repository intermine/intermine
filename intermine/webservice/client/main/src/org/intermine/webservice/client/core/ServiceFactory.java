package org.intermine.webservice.client.core;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.Model;
import org.intermine.webservice.client.services.ListService;
import org.intermine.webservice.client.services.ModelService;
import org.intermine.webservice.client.services.QueryService;
import org.intermine.webservice.client.services.TemplateService;
import org.intermine.webservice.client.services.WidgetService;

/**
 * The Class that should be used for creating services. You should never need to create instances of
 * service classes directly with their constructors - it is simpler to use this factory.
 * This factory deals with constructing services with the correct relative paths.
 *
 * <h2>Usage:</h2>
 *
 * <pre>
 *   ServiceFactory serviceFactory =
 *      new ServiceFactory("http://www.flymine.org/query/service", "MyApp")
 *   QueryService queryService = serviceFactory.getQueryService()
 *
 *      ...
 * </pre>
 *
 * <h2>Proxy settings:</h2>
 *
 * To configure access through a proxy, ensure that the following Java system
 * properties have been set:
 * <ul>
 * <li><code>http.proxyServer</code> (eg: "123.456.78.90")</li>
 * <li><code>http.proxyPort</code> (eg: "8080", Optional)</li>
 * </ul>
 *
 * @author Jakub Kulaviak
 *
 */
public class ServiceFactory
{

    private final String rootUrl;
    private String applicationName = "InterMine-WS-Client-Java-2.0";
    private final String userName;
    private final String userPass;
    private final String authToken;

    /**
     * Construct a service factory with a default application name. (defaults to
     * "InterMine-WS-Client-Java-$VERSION")
     * @param rootUrl the base URL for all services, it is the prefix common to all services.
     *      Example: http://www.flymine.org/query/service
     */
    public ServiceFactory(String rootUrl) {
        this.rootUrl = rootUrl;
        this.userName = null;
        this.userPass = null;
        this.authToken = null;
    }

    /**
     * Construct a factory for gaining access to specific resources. Allows you to set
     * the root url and the authorization token. Use this constructor if you need
     * to access private restricted-access resources.
     * @param rootUrl the base URL for all services, it is the prefix common to all services.
     *      Example: http://www.flymine.org/query/service
     * @param token the authorization token.
     */
    public ServiceFactory(String rootUrl, String token) {
        this.rootUrl = rootUrl;
        this.authToken = token;
        this.userName = null;
        this.userPass = null;
    }

    /**
     * Construct a factory for gaining access to specific resources. Allows you to set
     * the root url and the authorization token. Use this constructor if you need
     * to access private restricted-access resources.
     * @param rootUrl the base URL for all services, it is the prefix common to all services.
     *      Example: http://www.flymine.org/query/service
     * @param userName your user account name (usually an email address)
     * @param userPass your user account password, in plain text.
     *
     * <em>Please do not use this constructor unless you absolutely have to. Token
     * identification is preferred</em>
     * @deprecated This method causes username and password information to be insecurely
     * transmitted over HTTP connections. Use the token authentication mechanism instead.
     */
    @Deprecated
    public ServiceFactory(String rootUrl, String userName, String userPass) {
        this.rootUrl = rootUrl;
        this.authToken = null;
        this.userName = userName;
        this.userPass = userPass;
    }

    /**
     * Set the application name used to identify your requests to the server.
     * * @param name application name
     */
    public void setApplicationName(String name) {
        this.applicationName = name;
    }

    private void authoriseAndLink(Service s) {
        if (authToken != null) {
            s.setAuthentication(authToken);
        } else if (userName != null && userPass != null) {
            s.setAuthentication(userName, userPass);
        }
        s.setFactory(this);
    }

    // Variables for caching services. There is never going to be a need to create more
    // than one for any ServiceFactory.
    private QueryService qs;
    private TemplateService ts;
    private ListService ls;
    private ModelService ms;
    private WidgetService ws;

    /**
     * Return a new QueryService for getting query results from.
     * @return query service
     */
    public QueryService getQueryService() {
        if (qs == null) {
            qs = new QueryService(rootUrl, applicationName);
            authoriseAndLink(qs);
        }
        return qs;
    }

    /**
     * Return a new QueryService for getting query results from.
     * @return template service
     */
    public TemplateService getTemplateService() {
        if (ts == null) {
            ts = new TemplateService(rootUrl, applicationName);
            authoriseAndLink(ts);
        }
        return ts;
    }

    /**
     * Return a new ListService for getting getting list information from.
     * @return list service
     */
    public ListService getListService() {
        if (ls == null) {
            ls = new ListService(rootUrl, applicationName);
            authoriseAndLink(ls);
        }
        return ls;
    }

    /**
     * Return a new ModelService for retrieving the data model.
     * @return model service
     */
    public ModelService getModelService() {
        if (ms == null) {
            ms = new ModelService(rootUrl, applicationName);
        }
        return ms;
    }

    /**
     * A convenience method to getting a model (used widely throughout the API).
     * This method also allows us to not violate the law of Demeter in many
     * ugly ways.
     * @return the data model for the service we are attached to.
     */
    public Model getModel() {
        return getModelService().getModel();
    }

    /**
     * Creates a new service for general use.
     *
     * You very likely won't want to use this, unless you know exactly what resource you
     * are requesting.
     *
     * @param serviceRelativeUrl the part of the URL specific to this service
     *      Example: query/results
     * @param name application name, information for server which application uses this
     *      service
     * @return the created service
     */
    public Service getService(String serviceRelativeUrl, String name) {
        Service x = new Service(rootUrl, serviceRelativeUrl, name);
        authoriseAndLink(x);
        return x;
    }

    /**
     * Return the WidgetService for retrieving widget information.
     * @return widget service
     */
    public WidgetService getWidgetService() {
        if (ws == null) {
            ws = new WidgetService(rootUrl, applicationName);
            authoriseAndLink(ws);
        }
        return ws;
    }
}
