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
import java.util.Iterator;
import java.util.List;

import org.intermine.metadata.Model;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.webservice.client.core.ContentType;
import org.intermine.webservice.client.core.JSONResult;
import org.intermine.webservice.client.core.RequestImpl;
import org.intermine.webservice.client.core.Service;
import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.core.TabTableResult;
import org.intermine.webservice.client.core.Request.RequestType;
import org.intermine.webservice.client.exceptions.ServiceException;
import org.intermine.webservice.client.util.HttpConnection;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The QueryService is service that provides some methods for flexible querying InterMine data.
 *
 * <p>
 * The basic tool for querying data in InterMine is PathQuery object. See examples to see how
 * to construct PathQuery.
 * </p>
 *
 * @author Jakub Kulaviak
 **/
public class QueryService extends Service
{

    private static final String SERVICE_RELATIVE_URL = "query/results";


    /**
     * Use {@link ServiceFactory} instead for creating this service .
     * @param rootUrl root URL
     * @param applicationName application name
     */
    public QueryService(String rootUrl, String applicationName) {
        super(rootUrl, SERVICE_RELATIVE_URL, applicationName);
    }

    /**
     * A subclass of RequestImpl that described a request for a data query.
     *
     * @author Jakub Kulaviak
     */
    protected static class QueryRequest extends RequestImpl
    {
        /**
         * Constructor.
         *
         * @param type GET or POST
         * @param serviceUrl the URL of the service, without parameters
         * @param contentType a ContentType object
         */
        public QueryRequest(RequestType type, String serviceUrl, ContentType contentType) {
            super(type, serviceUrl, contentType);
        }

        /**
         * Sets the query that is to be executed.
         *
         * @param xml the query to be executed, in xml format
         */
        public void setQueryXml(String xml) {
            setParameter("query", xml);
        }
    }


    /**
     * Constructs PathQuery from its XML representation. You can use this method
     * for creating PathQuery, modifying it a bit and executing afterwards.
     * @param queryXml PathQuery represented as a XML string
     * @return created PathQuery
     */
    public PathQuery createPathQuery(String queryXml) {
        ModelService modelService = new ModelService(getRootUrl(), getApplicationName());
        Model model = modelService.getModel();
        Model.addModel(model.getName(), model);
        return PathQueryBinding.unmarshalPathQuery(new StringReader(queryXml),
                PathQuery.USERPROFILE_VERSION);
    }

    /**
     * Returns number of results of specified query.
     * @param query query
     * @return number of results of specified query.
     */
    public int getCount(PathQuery query) {
        return getCount(query.toXml(PathQuery.USERPROFILE_VERSION));
    }

    /**
     * Returns number of results of specified query.
     * @param queryXml PathQuery represented as a XML string
     * @return number of results of specified query.
     */
    public int getCount(String queryXml) {
        QueryRequest request = new QueryRequest(RequestType.POST, getUrl(), ContentType.TEXT_TAB);
        request.setQueryXml(queryXml);
        request.setParameter("tcount", "");
        String body = getResponseString(request);
        if (body.length() == 0) {
            throw new ServiceException("Service didn't return any result");
        }
        try {
            return Integer.parseInt(body);
        }  catch (NumberFormatException e) {
            throw new ServiceException("Service returned invalid result. It is not number: "
                    + body, e);
        }
    }
    
    /**
     * Fetch the results as a list of JSON Objects
     * @param query A PathQuery object
     * @return a list of JSON objects
     * @throws JSONException
     */
    public List<JSONObject> getJSONResults(PathQuery query) throws JSONException {
    	return getJSONResults(query.toXml(PathQuery.USERPROFILE_VERSION));	
    }
    
    /**
     * Fetch the results as a list of JSON Objects
     * @param queryXML An XML string representing the query
     * @return a list of JSON objects
     * @throws JSONException
     */
    public List<JSONObject> getJSONResults(String queryXml) throws JSONException {
    	QueryRequest request = new QueryRequest(RequestType.POST, getUrl(), ContentType.APPLICATION_JSON);
        request.setQueryXml(queryXml);
        request.setJSONFormat();
        JSONResult response = getJSONResponse(request);
        return response.getObjects();	
    }

    /**
     * Returns a String response for a request from a server.
     *
     * @param request the QueryRequest
     * @return a String
     */
    protected String getResponseString(QueryRequest request) {
        HttpConnection connection = executeRequest(request);
        return connection.getResponseBodyAsString().trim();
    }

    /**
     * Returns results of specified PathQuery. If you expect a lot of results
     * use getResultIterator() method.
     * @param query query
     * @param maxCount maximum number of returned results
     * @return results of specified PathQuery
     */
    public List<List<String>> getResult(PathQuery query, int maxCount) {
        return getResultInternal(query.toXml(PathQuery.USERPROFILE_VERSION), maxCount).getData();
    }

    /**
     * Returns results of specified PathQuery as iterator. Use this method if you expects a lot
     * of results and you would run out of memory.
     * @param query query
     * @param maxCount maximum number of returned results
     * @return results of specified PathQuery
     */
    public Iterator<List<String>> getResultIterator(PathQuery query, int maxCount) {
        return getResultInternal(query.toXml(PathQuery.USERPROFILE_VERSION), maxCount)
            .getIterator();
    }

    /**
     * Returns results of specified PathQuery. If you expect a lot of results
     * use getResultIterator() method.
     * @param queryXml PathQuery represented as a XML string
     * @param maxCount maximum number of returned results
     * @return results of specified PathQuery
     */
    public List<List<String>> getResult(String queryXml, int maxCount) {
        return getResultInternal(queryXml, maxCount).getData();
    }

    /**
     * Returns results of specified PathQuery. Use this method if you expects a lot
     * of results and you would run out of memory.
     * @param queryXml PathQuery represented as a XML string
     * @param maxCount maximum number of returned results
     * @return results of specified PathQuery
     */
    public Iterator<List<String>> getResultIterator(String queryXml, int maxCount) {
        return getResultInternal(queryXml, maxCount).getIterator();
    }

    private TabTableResult getResultInternal(String queryXml, int maxCount) {
        QueryRequest request = new QueryRequest(RequestType.POST, getUrl(),
                ContentType.TEXT_TAB);
        request.setMaxCount(maxCount);
        request.setQueryXml(queryXml);
        return getResponseTable(request);
    }

    /**
     * Performs the query and returns a TabTableResult containing the data.
     *
     * @param request a QueryRequest object
     * @return a TabTableResult object containing the data fetched
     */
    protected TabTableResult getResponseTable(QueryRequest request) {
        HttpConnection connection = executeRequest(request);
        return new TabTableResult(connection);
    }
    
    /**
     * Performs the query and returns a JSONResult containing the data.
     *
     * @param request a QueryRequest object
     * @return a JSONResult object containing the data fetched
     */
    protected JSONResult getJSONResponse(QueryRequest request) {
        HttpConnection connection = executeRequest(request);
        return new JSONResult(connection);
    }
    
}
