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

import java.util.Iterator;
import java.util.List;

import org.intermine.webservice.client.core.ContentType;
import org.intermine.webservice.client.core.JSONResult;
import org.intermine.webservice.client.core.RequestImpl;
import org.intermine.webservice.client.core.Service;
import org.intermine.webservice.client.core.TabTableResult;
import org.intermine.webservice.client.core.Request.RequestType;
import org.intermine.webservice.client.template.TemplateParameter;
import org.intermine.webservice.client.util.HttpConnection;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The TemplateService returns results of public templates and number of results. Template is
 * predefined query with some parameters. These parameters are variables that enable parameterized
 * queries. For example the same template can be run for Drosophila organism or Caenorhabditis.
 * It depends just at the specified organism parameter.
 *
 * @author Jakub Kulaviak
 **/
public class TemplateService extends Service
{

    private static final String SERVICE_RELATIVE_URL = "template/results";

    /**
     * Use {@link ServiceFactory} instead of constructor for creating this service.
     * @param rootUrl root URL
     * @param applicationName application name
     */
    public TemplateService(String rootUrl, String applicationName) {
        super(rootUrl, SERVICE_RELATIVE_URL, applicationName);
    }

    /**
     * A subclass of RequestImpl relevant to template queries.
     *
     * @author Jakub Kulaviak
     */
    protected static class TemplateRequest extends RequestImpl
    {
        /**
         * Constructor.
         *
         * @param type GET or POST
         * @param serviceUrl the URL of the service, not including parameters
         * @param contentType a ContentType object
         */
        public TemplateRequest(RequestType type, String serviceUrl, ContentType contentType) {
            super(type, serviceUrl, contentType);
        }

        /**
         * Sets the name of the template to be used.
         *
         * @param xml template name
         */
        public void setName(String xml) {
            setParameter("name", xml);
        }

        /**
         * Set some template parameters.
         *
         * @param parameters a List of TemplateParameter objects
         */
        public void setTemplateParameters(List<TemplateParameter> parameters) {
            for (int i = 0; i < parameters.size(); i++) {
                TemplateParameter par = parameters.get(i);
                int index = i + 1;
                addParameter("constraint" + index, par.getPathId());
                addParameter("op" + index, par.getOperation());
                addParameter("value" + index, par.getValue());
                if (par.getExtraValue() != null) {
                    addParameter("extra" + index, par.getExtraValue());
                }
                if (par.getCode() != null) {
                    addParameter("code" + index, par.getCode());
                }
            }
        }
    }

    /**
     * Returns template results for given parameters. If you expect a lot of results
     * use getResultIterator() method.
     * @param templateName template name
     * @param parameters parameters of template
     * @param maxCount maximum number of returned results
     * @see TemplateService
     * @return results
     */
    public List<List<String>> getResult(String templateName, List<TemplateParameter> parameters,
            int maxCount) {
        TemplateRequest request = new TemplateRequest(RequestType.POST, getUrl(),
                ContentType.TEXT_TAB);
        request.setMaxCount(maxCount);
        request.setName(templateName);
        request.setTemplateParameters(parameters);
        return getResponseTable(request).getData();
    }
    
    public List<JSONObject> getJSONResults(
    		String templateName, List<TemplateParameter> parameters, int maxCount) throws JSONException {
    	TemplateRequest request = new TemplateRequest(RequestType.POST, getUrl(),
                ContentType.TEXT_TAB);
        request.setMaxCount(maxCount);
        request.setJSONFormat();
        request.setName(templateName);
        request.setTemplateParameters(parameters);
        return getJSONResponse(request).getObjects();	
    }    

    /**
     * Returns template results for given parameters. Use this method if you expects a lot
     * of results and you would run out of memory.
     * @param templateName template name
     * @param parameters parameters of template
     * @param maxCount maximum number of returned results
     * @see TemplateService
     * @return results
     */
    public Iterator<List<String>> getResultIterator(String templateName,
            List<TemplateParameter> parameters, int maxCount) {
        TemplateRequest request = new TemplateRequest(RequestType.POST, getUrl(),
                ContentType.TEXT_TAB);
        request.setMaxCount(maxCount);
        request.setName(templateName);
        request.setTemplateParameters(parameters);
        return getResponseTable(request).getIterator();
    }

    /**
     * Performs the actual remote connection and fetches results.
     *
     * @param request a TemplateRequest object
     * @return a TabTableResult object containing the response data
     */
    protected TabTableResult getResponseTable(TemplateRequest request) {
        HttpConnection connection = executeRequest(request);
        return new TabTableResult(connection);
    }
    
    /**
     * Performs the query and returns a JSONResult containing the data.
     *
     * @param request a QueryRequest object
     * @return a JSONResult object containing the data fetched
     */
    protected JSONResult getJSONResponse(TemplateRequest request) {
        HttpConnection connection = executeRequest(request);
        return new JSONResult(connection);
    }
}
