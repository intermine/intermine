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
import org.intermine.webservice.client.core.Request.RequestType;
import org.intermine.webservice.client.core.RequestImpl;
import org.intermine.webservice.client.core.Service;
import org.intermine.webservice.client.core.XMLTableResult;
import org.intermine.webservice.client.exceptions.ServiceException;
import org.intermine.webservice.client.template.TemplateParameter;
import org.intermine.webservice.client.util.HttpConnection;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The TemplateService represents the connection to the resource that
 * returns results of templates and number of results.
 *
 * A template is a predefined query, with a set number of configurable parameters, similar to a
 * search form. Only a subset of their actual constraints are editable, although at least one will
 * be. For example you might have a template that finds Alcohol-Dehydrogenase genes in a specific
 * organism - although this would require a couple of constraints, only the one that
 * specifies the organism need be visible to the end user.
 *
 * From the user's perspective, templates can offer two advantages:
 * <ul>
 *   <li>They can be simpler to run, as only the parts of the query relevant to the particular
 *       search need to be specified (for example you never need to set the output columns)</li>
 *   <li>They provide simple access to a saved query from anywhere the webservice is available -
 *       so while queries can be saved as XML on your own machine, as a template they can be
 *       run from anywhere</li>
 * </ul>
 *
 * It is generally assumed that if you are using a template, you are familiar with its
 * parameters and output. These are not introspectable through the Java client, but you can
 * inspect them in the webapp of the InterMine data warehouse you are querying.
 *
 * usage:
 * <pre>
 * ServiceFactory serviceFactory = new ServiceFactory(serviceRootUrl);
 * TemplateService templateService = serviceFactory.getTemplateService();
 *
 * // Refer to the template by its name (displayed in the browser's address bar)
 * String templateName = "ChromLocation_GeneTranscriptExon";
 *
 * // Specify the values for this particular request
 * List<TemplateParameter> parameters = new ArrayList<TemplateParameter>();
 * parameters.add(new TemplateParameter("Chromosome.organism.name", "eq", "Drosophila melanogaster"));
 * parameters.add(new TemplateParameter("Chromosome.primaryIdentifier", "eq", "2L"));
 * parameters.add(new TemplateParameter("Chromosome.locatedFeatures.start", "ge", "1"));
 * parameters.add(new TemplateParameter("Chromosome.locatedFeatures.end", "lt", "10000"));
 *
 * List<List<String>> result = service.getAllResults(templateName, parameters);
 * System.out.print("Results: \n");
 * for (List<String> row : result) {
 *     for (String cell : row) {
 *         System.out.print(cell + " ");
 *     }
 *     System.out.print("\n");
 * }
 * </pre>
 *
 * @author Jakub Kulaviak
 * @author Alexis Kalderimis
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
            setXMLFormat();
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
     * Returns the number of results the specified template will return.
     *
     * @param templateName the name of the template to run
     * @param parameters the values for the templates editable constraints
     * @return number of results of specified query.
     */
    public int getCount(String templateName, List<TemplateParameter> parameters) {
        TemplateRequest request =
            new TemplateRequest(RequestType.POST, getUrl(), ContentType.TEXT_COUNT);

        request.setName(templateName);
        request.setTemplateParameters(parameters);
        String body = getStringResponse(request);
        if (body.length() == 0) {
            throw new ServiceException("The server didn't return any results");
        }
        try {
            return Integer.parseInt(body);
        }  catch (NumberFormatException e) {
            throw new ServiceException("The server returned an invalid result. It is not a number: "
                    + body, e);
        }
    }

    /**
     * Returns all the results for the given template template with the given parameters.
     * If you expect a lot of results we would recommend you use getAllResultIterator() method.
     *
     * @param templateName template name
     * @param parameters parameters of template
     *
     * @return results
     */
    public List<List<String>> getAllResults(String templateName,
            List<TemplateParameter> parameters) {
        TemplateRequest request =
                new TemplateRequest(RequestType.POST, getUrl(), ContentType.TEXT_TAB);
        request.setName(templateName);
        request.setTemplateParameters(parameters);
        return getResponseTable(request).getData();
    }

    /**
     * Returns a set of the results for the given template template with the given parameters,
     * defined by the index of the first result you want back, and the maximum page size.
     * If you expect a lot of results we would recommend you use getResultIterator() method.
     *
     * @param templateName template name
     * @param parameters parameters of template
     * @param start the index of the first result to return
     * @param maxCount maximum number of returned results
     *
     * @return results
     */
    public List<List<String>> getResults(String templateName, List<TemplateParameter> parameters,
            int start, Integer maxCount) {
        TemplateRequest request =
                new TemplateRequest(RequestType.POST, getUrl(), ContentType.TEXT_TAB);
        request.setName(templateName);
        request.setTemplateParameters(parameters);
        request.setStart(start);
        if (maxCount != null) {
            request.setMaxCount(maxCount);
        }
        return getResponseTable(request).getData();
    }

    /**
     * Returns all the results for the given template template with the given parameters,
     * as JSON objects (see @link {http://www.intermine.org/wiki/JSONObjectFormat}).
     *
     * @param templateName template name
     * @param parameters parameters of template
     *
     * @return results
     *
     * @throws JSONException if the server returns content that cannot be parsed as JSON
     */
    public List<JSONObject> getAllJSONResults(String templateName,
            List<TemplateParameter> parameters) throws JSONException {
        TemplateRequest request =
                new TemplateRequest(RequestType.POST, getUrl(), ContentType.TEXT_TAB);
        request.setJSONFormat();
        request.setName(templateName);
        request.setTemplateParameters(parameters);
        return getJSONResponse(request).getObjects();
    }


    /**
     * Returns a subset of the results for the given template template with the given parameters,
     * as JSON objects (see @link {http://www.intermine.org/wiki/JSONObjectFormat}).
     *
     * @param templateName template name
     * @param parameters parameters of template
     * @param start the index of the first result to return
     * @param maxCount maximum number of returned results
     *
     * @return a list of JSON objects
     *
     * @throws JSONException if the server returns content that cannot be parsed as JSON
     */
    public List<JSONObject> getJSONResults(String templateName,
            List<TemplateParameter> parameters, int start, Integer maxCount) throws JSONException {
        TemplateRequest request =
                new TemplateRequest(RequestType.POST, getUrl(), ContentType.TEXT_TAB);
        request.setJSONFormat();
        request.setName(templateName);
        request.setTemplateParameters(parameters);
        request.setStart(start);
        if (maxCount != null) {
            request.setMaxCount(maxCount);
        }
        return getJSONResponse(request).getObjects();
    }

    /**
     * Returns all the rows for the template when run with the given parameters.
     *
     * Use this method if you expect a lot of results and you would otherwise run out of memory.
     *
     * @param templateName template name
     * @param parameters parameters of template
     *
     * @return results as an iterator over lists of strings
     */
    public Iterator<List<String>> getAllResultIterator(String templateName,
            List<TemplateParameter> parameters) {
        TemplateRequest request =
                new TemplateRequest(RequestType.POST, getUrl(), ContentType.TEXT_TAB);
        request.setName(templateName);
        request.setTemplateParameters(parameters);
        return getResponseTable(request).getIterator();
    }

    /**
     * Returns a subset of rows for the template when run with the given parameters.
     *
     * Use this method if you expect a lot of results and you would otherwise run out of memory.
     *
     * @param templateName template name
     * @param parameters parameters of template
     * @param start the index of the first result to return
     * @param maxCount maximum number of returned results
     *
     * @return results as an iterator over lists of strings
     */
    public Iterator<List<String>> getResultIterator(String templateName,
            List<TemplateParameter> parameters, int start, Integer maxCount) {
        TemplateRequest request =
                new TemplateRequest(RequestType.POST, getUrl(), ContentType.TEXT_TAB);
        request.setName(templateName);
        request.setTemplateParameters(parameters);
        request.setStart(start);
        if (maxCount != null) {
            request.setMaxCount(maxCount);
        }
        return getResponseTable(request).getIterator();
    }

    /**
     * Performs the actual remote request and fetches the results.
     *
     * @param request a TemplateRequest object
     * @return a XMLTableResult object containing the response data
     */
    protected XMLTableResult getResponseTable(TemplateRequest request) {
        HttpConnection connection = executeRequest(request);
        return new XMLTableResult(connection);
    }

    /**
     * Performs the request and returns a JSONResult containing the data.
     *
     * @param request a QueryRequest object
     * @return a JSONResult object containing the data fetched
     */
    protected JSONResult getJSONResponse(TemplateRequest request) {
        HttpConnection connection = executeRequest(request);
        return new JSONResult(connection);
    }

    /**
     * Performs the request and returns the result as a string.
     * @param request The TemplateRequest object
     * @return a string containing the body of the response
     */
    protected String getStringResponse(TemplateRequest request) {
        HttpConnection connection = executeRequest(request);
        return connection.getResponseBodyAsString().trim();
    }
}
