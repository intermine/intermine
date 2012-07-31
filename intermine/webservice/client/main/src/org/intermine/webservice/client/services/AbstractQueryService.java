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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.webservice.client.core.Request;
import org.intermine.webservice.client.core.Service;
import org.intermine.webservice.client.results.JSONResult;
import org.intermine.webservice.client.results.Page;
import org.intermine.webservice.client.results.RowResultSet;
import org.intermine.webservice.client.results.XMLTableResult;
import org.intermine.webservice.client.util.HttpConnection;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Common behaviour for services requesting results from a web-service.
 * @author Alex Kalderimis
 *
 * @param <T> The kind of query this service supports.
 */
public abstract class AbstractQueryService<T> extends Service
{
    /**
     * Constructor.
     * @param rootUrl The root URL to the service provider
     * @param serviceRelativeUrl The path to the resource.
     * @param applicationName The name of the application.
     */
    public AbstractQueryService(String rootUrl, String serviceRelativeUrl, String applicationName) {
        super(rootUrl, serviceRelativeUrl, applicationName);
    }

    // ABSTRACT METHODS THAT THE OTHERS DEPEND ON

    /**
     * Returns an iterator over a subset of rows for the template.
     *
     * Use this method if you expect a lot of results and you would otherwise
     * run out of memory.
     *
     * @param query The query to run.
     * @param page The subsection of the result set to retrieve.
     *
     * @return results as an iterator over lists of strings
     */
    abstract Iterator<List<String>> getRowIterator(T query,  Page page);

    /**
     * Returns an iterator over a subset of rows for the template.
     *
     * Use this method if you expect a lot of results and you would otherwise
     * run out of memory.
     *
     * @param query The template to run.
     * @param page The subsection of the result set to retrieve.
     *
     * @return results as an iterator over lists of strings
     */
    abstract RowResultSet getRows(T query, Page page);

    /**
     * Returns a subset of the results for the given template
     * as JSON objects (see @link {http://www.intermine.org/wiki/JSONObjectFormat}).
     *
     * @param query The query to run.
     * @param page The subsection of the result set to retrieve.
     *
     * @return a list of JSON objects
     *
     * @throws JSONException if the server returns content that cannot be parsed as JSON
     */
    abstract List<JSONObject> getJSONResults(T query, Page page) throws JSONException;

    /**
     * Returns the number of rows the specified template will return.Re
     * @param query The query to run.
     * @return The number of rows it will return.
     */
    abstract int getCount(T query);

    /**
     * Returns a set of the results for the given template
     * defined by the index of the first result you want back, and the maximum page size.
     * If you expect a lot of results we would recommend you use getResultIterator() method.
     *
     * @param query The query to run.
     * @param page The subsection of the result set to retrieve.
     *
     * @return A list of rows, where each row is list of strings.
     */
    abstract List<List<String>> getResults(T query, Page page);

    // EVERYTHING THAT WE CAN DO WITH THESE ABSTRACT METHODS...

    /**
     * Returns all the results for the given template template.
     * If you expect a lot of results we would recommend you use getAllResultIterator() method.
     *
     * @param query The template to run.
     *
     * @return results
     */
    public List<List<String>> getAllResults(T query) {
        return getResults(query, Page.DEFAULT);
    }

    /**
     * Returns all the results for the given template template,
     * as JSON objects (see @link {http://www.intermine.org/wiki/JSONObjectFormat}).
     *
     * @param query The query to run.
     *
     * @return The results for this template.
     *
     * @throws JSONException if the server returns content that cannot be parsed as JSON
     */
    public List<JSONObject> getAllJSONResults(T query) throws JSONException {
        return getJSONResults(query, Page.DEFAULT);
    }

    /**
     * Returns an iterator over all the rows for the template.
     *
     * Use this method if you expect a lot of results and you would otherwise run out of memory.
     *
     * @param query The template to run.
     *
     * @return results as an iterator over lists of strings
     */
    public Iterator<List<String>> getAllRowsIterator(T query) {
        return getRowIterator(query, Page.DEFAULT);
    }

    /**
     * Get an iterator over the results of a query. The iterator returns a representation
     * of one row at a time, in the order received over the connection,
     * up to the maximum result size of 10,000,000 rows from the beginning.
     *
     * @param query the query to run.
     *
     * @return an iterator over the rows, where each row is a mapping from output column to value.
     */
    public Iterator<Map<String, Object>> getRowMapIterator(T query) {
        return getRows(query, Page.DEFAULT).getMapIterator();
    }

    /**
     * Get an iterator over the results of a query. The iterator returns a representation
     * of one row at a time, in the order received over the connection.
     *
     * @param query the query to run.
     * @param page The subsection of the result set to retrieve.
     * @return an iterator over the rows, where each row is a mapping from output column to value.
     */
    public Iterator<Map<String, Object>> getRowMapIterator(T query, Page page) {
        return getRows(query, page).getMapIterator();
    }

    /**
     * Get an iterator over the results of a query. The iterator returns a representation
     * of one row at a time, in the order received over the connection. Retrieves up to the maximum
     * result size of 10,000,000 rows from the beginning.
     *
     * @param query the query to run.
     *
     * @return an iterator over the rows, where each row is a list of objects.
     */
    public Iterator<List<Object>> getRowListIterator(T query) {
        return getRows(query, Page.DEFAULT).getListIterator();
    }

    /**
     * Get an iterator over the results of a query. The iterator returns a representation
     * of one row at a time, in the order received over the connection.
     *
     * @param query the query to run.
     * @param page The subsection of the result set to retrieve.
     * @return an iterator over the rows, where each row is a list of objects.
     */
    public Iterator<List<Object>> getRowListIterator(T query, Page page) {
        return getRows(query, page).getListIterator();
    }

    /**
     * Get results for a query as rows of objects. Get up to the
     * maximum result size of 10,000,000 rows from the beginning.
     *
     * @param query The query to run.
     *
     * @return a list of rows, which are each a map from output
     * column (in alternate long and short form) to value.
     */
    public List<Map<String, Object>> getRowsAsMaps(T query) {
        return getRows(query, Page.DEFAULT).getRowsAsMaps();
    }

    /**
     * Get results for a query as rows of objects.
     *
     * @param query The query to run.
     * @param page The subsection of the result set to retrieve.
     * @return a list of rows, which are each a map from output
     * column (in alternate long and short form) to value.
     */
    public List<Map<String, Object>> getRowsAsMaps(T query, Page page) {
        return getRows(query, page).getRowsAsMaps();
    }


    /**
     * Get results for a query as rows of objects. Retrieve up to
     * 10,000,000 rows from the beginning.
     *
     * @param query The query to run.
     *
     * @return a list of rows, which are each a list of cells.
     */
    public List<List<Object>> getRowsAsLists(T query) {
        return getRows(query, Page.DEFAULT).getRowsAsLists();
    }

    /**
     * Get results for a query as rows of objects.
     *
     * @param query The query to run.
     * @param page The subsection of the result set to retrieve.
     * @return a list of rows, which are each a list of cells.
     */
    public List<List<Object>> getRowsAsLists(T query, Page page) {
        return getRows(query, page).getRowsAsLists();
    }

    /**
     * Performs the actual remote request and fetches the results.
     *
     * @param request a Request object
     * @return a XMLTableResult object containing the response data
     */
    protected XMLTableResult getResponseTable(Request request) {
        HttpConnection connection = executeRequest(request);
        return new XMLTableResult(connection);
    }

    /**
     * Performs the actual remote request and fetches the results.
     *
     * @param request a Request object
     * @param views The list of the output columns
     * @return a RowResultSet object containing the response data
     */
    protected RowResultSet getRows(Request request, List<String> views) {
        HttpConnection connection = executeRequest(request);
        return new RowResultSet(connection, views, getAPIVersion());
    }

    /**
     * Performs the request and returns a JSONResult containing the data.
     *
     * @param request a QueryRequest object
     * @return a JSONResult object containing the data fetched
     */
    protected JSONResult getJSONResponse(Request request) {
        HttpConnection connection = executeRequest(request);
        return new JSONResult(connection);
    }


}
