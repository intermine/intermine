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
import org.intermine.webservice.client.core.Request.RequestType;
import org.intermine.webservice.client.core.RequestImpl;
import org.intermine.webservice.client.core.Service;
import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.core.TabTableResult;
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

    private static final int START = 0;

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
        QueryRequest request = new QueryRequest(RequestType.POST, getUrl(), ContentType.TEXT_COUNT);
        request.setQueryXml(queryXml);
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
        return getJSONResults(query.toXml(PathQuery.USERPROFILE_VERSION), START, null);
    }

    /**
     * Fetch the results as a list of JSON Objects
     * @param query A PathQuery object
     * @param maxCount maximum number of returned results
     * @return a list of JSON objects
     * @throws JSONException
     */
    public List<JSONObject> getJSONResults(PathQuery query, Integer maxCount) throws JSONException {
        return getJSONResults(query.toXml(PathQuery.USERPROFILE_VERSION), START, maxCount);
    }

    /**
     * Fetch the results as a list of JSON Objects
     * @param query A PathQuery object
     * @param start the starting index
     * @param maxCount maximum number of returned results
     * @return a list of JSON objects
     * @throws JSONException
     */
    public List<JSONObject> getJSONResults(PathQuery query, int start, Integer maxCount)
        throws JSONException {
        return getJSONResults(query.toXml(PathQuery.USERPROFILE_VERSION), start, maxCount);
    }

    /**
     * Fetch the results as a list of JSON Objects
     * @param queryXml An XML string representing the query
     * @return a list of JSON objects
     * @throws JSONException
     */
    public List<JSONObject> getJSONResults(String queryXml) throws JSONException {
        return getJSONResults(queryXml, START, null);
    }

    /**
     * Fetch the results as a list of JSON Objects
     * @param queryXml An XML string representing the query
     * @param maxCount maximum number of returned results
     * @return a list of JSON objects
     * @throws JSONException
     */
    public List<JSONObject> getJSONResults(String queryXml, Integer maxCount) throws JSONException {
        return getJSONResults(queryXml, START, maxCount);
    }

    /**
     * Fetch the results as a list of JSON Objects
     * @param queryXml An XML string representing the query
     * @param start the starting index
     * @param size the maximum number of results to return
     * @return a list of JSON objects
     * @throws JSONException
     */
    public List<JSONObject> getJSONResults(String queryXml, int start, Integer size)
        throws JSONException {
        QueryRequest request
            = new QueryRequest(RequestType.POST, getUrl(), ContentType.APPLICATION_JSON);
        request.setQueryXml(queryXml);
        request.setJSONFormat();
        request.setStart(start);
        if (size != null) {
            request.setMaxCount(size);
        }
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
     * @param start The starting index
     * @param maxCount maximum number of returned results
     * @return results of specified PathQuery
     */
    public List<List<String>> getResult(PathQuery query, int start, Integer maxCount) {
        return getResultInternal(
                query.toXml(PathQuery.USERPROFILE_VERSION), start, maxCount).getData();
    }

    /**
     * Returns results of specified PathQuery. If you expect a lot of results
     * use getResultIterator() method.
     * @param query query
     * @param maxCount maximum number of returned results
     * @return results of specified PathQuery
     */
    public List<List<String>> getResult(PathQuery query, Integer maxCount) {
        return getResultInternal(
                query.toXml(PathQuery.USERPROFILE_VERSION), START, maxCount).getData();
    }

    /**
     * Returns results of specified PathQuery. If you expect a lot of results
     * use getResultIterator() method.
     * @param query query
     * @return results of specified PathQuery
     */
    public List<List<String>> getResult(PathQuery query) {
        return getResultInternal(query.toXml(PathQuery.USERPROFILE_VERSION), START, null).getData();
    }

    /**
     * Returns results of specified PathQuery as iterator. Use this method if you expect a lot
     * of results and you would run out of memory.
     * @param query query
     * @param start The starting index
     * @param maxCount maximum number of returned results
     * @return results of specified PathQuery
     */
    public Iterator<List<String>> getResultIterator(PathQuery query, int start, Integer maxCount) {
        return getResultInternal(query.toXml(PathQuery.USERPROFILE_VERSION), start, maxCount)
            .getIterator();
    }

    /**
     * Returns results of specified PathQuery as iterator. Use this method if you expect a lot
     * of results and you would run out of memory.
     * @param query query
     * @param maxCount maximum number of returned results
     * @return results of specified PathQuery
     */
    public Iterator<List<String>> getResultIterator(PathQuery query, Integer maxCount) {
        return getResultIterator(query, START, maxCount);
    }

    /**
     * Returns results of specified PathQuery as iterator. Use this method if you expect a lot
     * of results and you would run out of memory.
     * @param query query
     * @return results of specified PathQuery
     */
    public Iterator<List<String>> getResultIterator(PathQuery query) {
        return getResultIterator(query, START, null);
    }

    /**
     * Returns results of specified PathQuery. If you expect a lot of results
     * use getResultIterator() method.
     * @param queryXml PathQuery represented as a XML string
     * @param start the starting index
     * @param maxCount maximum number of returned results
     * @return results of specified PathQuery
     */
    public List<List<String>> getResult(String queryXml, int start, Integer maxCount) {
        return getResultInternal(queryXml, start, maxCount).getData();
    }

    /**
     * Returns results of specified PathQuery. If you expect a lot of results
     * use getResultIterator() method.
     * @param queryXml PathQuery represented as a XML string
     * @param maxCount maximum number of returned results
     * @return results of specified PathQuery
     */
    public List<List<String>> getResult(String queryXml, Integer maxCount) {
        return getResultInternal(queryXml, START, maxCount).getData();
    }

    /**
     * Returns results of specified PathQuery. If you expect a lot of results
     * use getResultIterator() method.
     * @param queryXml PathQuery represented as a XML string
     * @return results of specified PathQuery
     */
    public List<List<String>> getResult(String queryXml) {
        return getResultInternal(queryXml, START, null).getData();
    }

    /**
     * Returns results of specified PathQuery. Use this method if you expect a lot
     * of results and you would run out of memory.
     * @param queryXml PathQuery represented as a XML string
     * @param start the starting index
     * @param maxCount maximum number of returned results
     * @return results of specified PathQuery
     */
    public Iterator<List<String>> getResultIterator(String queryXml, int start, Integer maxCount) {
        return getResultInternal(queryXml, start, maxCount).getIterator();
    }

    /**
     * Returns results of specified PathQuery. Use this method if you expect a lot
     * of results and you would run out of memory.
     * @param queryXml PathQuery represented as a XML string
     * @param maxCount The maximum number of results to return
     * @return results of specified PathQuery
     */
    public Iterator<List<String>> getResultIterator(String queryXml, Integer maxCount) {
        return getResultIterator(queryXml, START, maxCount);
    }

    /**
     * Returns results of specified PathQuery. Use this method if you expect a lot
     * of results and you would run out of memory.
     * @param queryXml PathQuery represented as a XML string
     * @return results of specified PathQuery
     */
    public Iterator<List<String>> getResultIterator(String queryXml) {
        return getResultIterator(queryXml, START, null);
    }

    private TabTableResult getResultInternal(String queryXml, int start, Integer maxCount) {
        QueryRequest request = new QueryRequest(RequestType.POST, getUrl(),
                ContentType.TEXT_TAB);
        if (maxCount != null) {
            request.setMaxCount(maxCount);
        }
        request.setStart(start);
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
