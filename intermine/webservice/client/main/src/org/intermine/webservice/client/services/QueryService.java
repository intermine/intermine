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
import java.util.Map;

import org.intermine.metadata.Model;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.webservice.client.core.ContentType;
import org.intermine.webservice.client.core.JSONResult;
import org.intermine.webservice.client.core.Request.RequestType;
import org.intermine.webservice.client.core.RequestImpl;
import org.intermine.webservice.client.core.Service;
import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.core.XMLTableResult;
import org.intermine.webservice.client.core.RowResultSet;
import org.intermine.webservice.client.exceptions.ServiceException;
import org.intermine.webservice.client.util.HttpConnection;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The QueryService class provides a mechanism for sending data queries to an InterMine
 * webservice, and retrieving and parsing the results.
 *
 * <p>
 * The basic tool for querying data in InterMine is the PathQuery object. PathQueries are
 * highly abstracted structured representations of a query over a database, including
 * ways to select the output columns and the filters. To see examples to see how
 * to construct PathQuery objects, see {@link PathQuery}.
 * </p>
 *
 * Usage:
 * <pre>
 * ServiceFactory serviceFactory = new ServiceFactory(serviceRootUrl);
 * QueryService queryService = serviceFactory.getQueryService();
 * ModelService modelService = serviceFactory.getModelService();
 * PathQuery query = new PathQuery(modelService.getModel());
 *
 * query.addViews("Gene.symbol", "Gene.length", "Gene.proteins.symbol");
 * query.addConstraint(Constraints.lookup("Gene", "zen", "D. melanogaster"));
 *
 * //find out how many results there are
 * int count = queryService.getCount(query);
 *
 * System.out.println("There are " + count + " results for this query");
 *
 * List<List<String>> rows = service.getAllResults(query);
 *
 * for (List<String> row : rows) {
 *      for (int i = 0; i < row.size(); i++) {
 *           System.out.println(query.getViews().get(i) + " is " + row.get(i));
 *      }
 *      System.out.print("\n");
 * }
 *
 * </pre>
 *
 * Query results are typically fetched as a multi-dimensional list of strings (rows of columns), but
 * you can also request simply the count of result rows in the whole result set, and JSONObjects,
 * a nested representation of individual records
 * (@link {http://www.intermine.org/wiki/JSONObjectFormat}).
 *
 * Results can also be paged: passing in the start index and the page size will return the
 * appropriate slice of results. The default start is 0 (the beginning) and the default page-size
 * is null (everything). So the query above will get all results.
 *
 * @author Jakub Kulaviak
 **/
public class QueryService extends Service
{

    private static final String SERVICE_RELATIVE_URL = "query/results";

    private static final int START = 0;

    private static final Integer ALL_RESULTS = null;

    /**
     * Constructor. It is recommended for maintainability reasons that you not use this constructor.
     * Instead, use the {@link ServiceFactory} instead for creating instances of this class.
     * @param rootUrl root URL
     * @param applicationName application name
     */
    public QueryService(String rootUrl, String applicationName) {
        super(rootUrl, SERVICE_RELATIVE_URL, applicationName);
    }

    /**
     * The request class for handling queries - in particular this class is responsible
     * for handling the query parameters. The only special one that query supports is "query"
     *
     *  @author Jakub Kulaviak
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
            setXMLFormat();
        }

        /**
         * Adds the query to be run. The query is passed to the service as a string, in XML
         * format.
         *
         * @param xml the query to be executed, in XML format
         */
        public void setQueryXml(String xml) {
            setParameter("query", xml);
        }
    }


    /**
     * Constructs a PathQuery from its XML representation. You can use this method
     * for creating a PathQuery, modifying it a bit and then executing it afterwards.
     * This might be useful if you have saved some useful queries as XML to local files
     * and wish to adjust some of their parameters in Java code.
     *
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
     * Returns the number of results the specified query will return.
     *
     * @param query query
     * @return number of results of specified query.
     */
    public int getCount(PathQuery query) {
        return getCount(query.toXml(PathQuery.USERPROFILE_VERSION));
    }

    /**
     * Returns the number of results the specified query will return.
     *
     * @param queryXml PathQuery represented as a XML string
     * @return number of results of specified query.
     */
    public int getCount(String queryXml) {
        QueryRequest request = new QueryRequest(RequestType.POST, getUrl(), ContentType.TEXT_COUNT);
        request.setQueryXml(queryXml);
        request.setCountFormat();
        String body = getResponseString(request);
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
     * Fetch all the results as a list of JSON Objects
     *
     * @param query A PathQuery object
     *
     * @return a list of JSON objects
     *
     * @throws JSONException if the server returns badly formatted JSON, or non-json data
     */
    public List<JSONObject> getAllJSONResults(PathQuery query) throws JSONException {
        return getJSONResults(query.toXml(PathQuery.USERPROFILE_VERSION), START, null);
    }

    /**
     * Fetch all the results as a list of JSON Objects
     *
     * @param queryXml An XML string representing the query
     *
     * @return a list of JSON objects
     *
     * @throws JSONException if the server returns badly formatted JSON, or non-JSON data
     */
    public List<JSONObject> getAllJSONResults(String queryXml) throws JSONException {
        return getJSONResults(queryXml, START, ALL_RESULTS);
    }

    /**
     * Fetch some results as a list of JSON Objects. You define which results you want
     * by supplying a start index, and a maximum page size.
     *
     * @param query A PathQuery object
     * @param start the starting index
     * @param maxCount maximum number of returned results
     *
     * @return a list of JSON objects
     *
     * @throws JSONException if the server returns badly formatted JSON, or non-JSON data
     */
    public List<JSONObject> getJSONResults(PathQuery query, int start, Integer maxCount)
        throws JSONException {
        return getJSONResults(query.toXml(PathQuery.USERPROFILE_VERSION), start, maxCount);
    }

    /**
     * Fetch some results as a list of JSON Objects. You define which results you want
     * by supplying a start index, and a maximum page size.
     *
     * @param queryXml An XML string representing the query
     * @param start the starting index
     * @param size the maximum number of results to return
     *
     * @return a list of JSON objects
     *
     * @throws JSONException if the server returns badly formatted JSON, or non-JSON data
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
     * Returns a String response for a request from a server. This is the method used internally
     * to actually perform requests to the server.
     *
     * @param request the QueryRequest
     *
     * @return a String
     */
    protected String getResponseString(QueryRequest request) {
        HttpConnection connection = executeRequest(request);
        return connection.getResponseBodyAsString().trim();
    }

    /**
     * Returns a specific set of the results of the specified PathQuery.
     * If you expect a lot of results, or have set a very large page size
     * we would recommend that you use the getResultIterator() method instead, as that will
     * read through the lines one by one without storing them all in memory at once.
     *
     * @param query the query as a PathQuery object
     *
     * @param start The starting index
     * @param maxCount maximum number of returned results
     *
     * @return results of specified PathQuery
     */
    public List<List<String>> getResults(PathQuery query, int start, Integer maxCount) {
        return getResultInternal(
                query.toXml(PathQuery.USERPROFILE_VERSION), start, maxCount).getData();
    }

    /**
     * Returns a specific set of the results of the specified PathQuery.
     * If you expect a lot of results, or have set a very large page size
     * we would recommend that you use the getResultIterator() method instead, as that will
     * read through the lines one by one without storing them all in memory at once.
     *
     * @param queryXml PathQuery represented as a XML string
     * @param start the starting index
     * @param maxCount maximum number of returned results (null means all results)
     *
     * @return results of specified PathQuery
     */
    public List<List<String>> getResults(String queryXml, int start, Integer maxCount) {
        return getResultInternal(queryXml, start, maxCount).getData();
    }

    /**
     * Returns all of the results of the specified PathQuery.
     * If you expect a lot of results, we would recommend that you use the getResultIterator()
     * method instead, as that will read through the lines one by one without storing
     * them all in memory at once.
     *
     * Returns results of specified PathQuery. If you expect a lot of results
     * use getResultIterator() method.
     * @param query query
     * @return results of specified PathQuery
     */
    public List<List<String>> getAllResults(PathQuery query) {
        return getResultInternal(query.toXml(PathQuery.USERPROFILE_VERSION), START, null).getData();
    }

    /**
     * Returns all of the results of the specified PathQuery.
     * If you expect a lot of results, we would recommend that you use the getResultIterator()
     * method instead, as that will read through the lines one by one without storing
     * them all in memory at once.
     *
     * @param queryXml PathQuery represented as a XML string
     *
     * @return results of specified PathQuery
     */
    public List<List<String>> getAllResults(String queryXml) {
        return getResultInternal(queryXml, START, null).getData();
    }

    /**
     * Returns a specific set of the results of a specified PathQuery as an iterator.
     * We would recommend that you use this method if you expect a lot of rows of results
     * and might run out of memory.
     *
     * @param query the query as a PathQuery object
     * @param start The starting index
     * @param maxCount maximum number of returned results
     *
     * @return results of specified PathQuery
     */
    public Iterator<List<String>> getResultIterator(PathQuery query, int start, Integer maxCount) {
        return getResultInternal(query.toXml(PathQuery.USERPROFILE_VERSION), start, maxCount)
            .getIterator();
    }

    /**
     * Returns a specific set of the results of a specified PathQuery as an iterator.
     * We would recommend that you use this method if you expect a lot of rows of results and
     * might run out of memory.
     *
     * @param queryXml the query represented as an XML string
     * @param start the starting index
     * @param maxCount maximum number of returned results
     *
     * @return results of specified PathQuery
     */
    public Iterator<List<String>> getResultIterator(String queryXml, int start, Integer maxCount) {
        return getResultInternal(queryXml, start, maxCount).getIterator();
    }

    /**
     * Returns all the results of a specified PathQuery as an iterator. We would recommend that
     * you use this method if you expect a lot of rows of results and might run out of memory.
     *
     * @param query the query as a PathQuery object
     *
     * @return results of specified PathQuery
     */
    public Iterator<List<String>> getAllResultIterator(PathQuery query) {
        return getResultIterator(query, START, null);
    }

    /**
     * Returns all the results of a specified PathQuery as an iterator. We would recommend that
     * you use this method if you expect a lot of rows of results and might run out of memory.
     *
     * @param queryXml the query represented as an XML string
     *
     * @return an iterator over the results of the specified PathQuery
     */
    public Iterator<List<String>> getAllResultIterator(String queryXml) {
        return getResultIterator(queryXml, START, null);
    }

    /**
     * Prepare the request by setting the appropriate parameters onto the request object, before
     * sending it off to receive the results.
     *
     * @param queryXml the query represented as an XML string
     * @param start the starting index
     * @param maxCount maximum number of returned results
     *
     * @return a data table object
     */
    private XMLTableResult getResultInternal(String queryXml, int start, Integer maxCount) {
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
     * Performs the query and returns a XMLTableResult containing the data.
     * This is an internal method for handling the intermediate processing steps
     *
     * @param request a QueryRequest object
     *
     * @return a XMLTableResult object containing the data fetched
     */
    protected XMLTableResult getResponseTable(QueryRequest request) {
        HttpConnection connection = executeRequest(request);
        return new XMLTableResult(connection);
    }

    /**
     * Get results for a query as rows of objects. 
     *
     * @param query The query to run.
     * @param start The first index to run.
     * @param maxCount The maximum size of the result set (or 10,000,000 if null)
     *
     * @return a list of rows, which are each a list of cells.
     */
    public List<List<Object>> getRowsAsLists(String query, int start, Integer maxCount) {
        return getRows(query, start, maxCount).getRowsAsLists();
    }

    /**
     * Get results for a query as rows of objects. 
     *
     * @param query The query to run.
     * @param start The first index to run.
     * @param maxCount The maximum size of the result set (or 10,000,000 if null)
     *
     * @return a list of rows, which are each a list of cells.
     */
    public List<List<Object>> getRowsAsLists(PathQuery query, int start, Integer maxCount) {
        return getRows(query, start, maxCount).getRowsAsLists();
    }

    /**
     * Get results for a query as rows of objects. Retrieve up to 10,000,000 result from the given starting point.
     *
     * @param query The query to run.
     * @param start The first index to run.
     *
     * @return a list of rows, which are each a list of cells.
     */
    public List<List<Object>> getRowsAsLists(String query, int start) {
        return getRows(query, start, null).getRowsAsLists();
    }

    /**
     * Get results for a query as rows of objects. Retrieve up to 10,000,000 result from the given starting point.
     *
     * @param query The query to run.
     * @param start The index of the first result to include.
     *
     * @return a list of rows, which are each a list of cells.
     */
    public List<List<Object>> getRowsAsLists(PathQuery query, int start) {
        return getRows(query, start, null).getRowsAsLists();
    }
    
    /**
     * Get results for a query as rows of objects. Retrieve up to 10,000,000 results from the beginning.
     *
     * @param query The query to run.
     *
     * @return a list of rows, which are each a list of cells.
     */
    public List<List<Object>> getRowsAsLists(String query) {
        return getRows(query, 0, null).getRowsAsLists();
    }

    /**
     * Get results for a query as rows of objects. Retrieve up to 10,000,000 results from the beginning.
     *
     * @param query The query to run.
     *
     * @return a list of rows, which are each a list of cells.
     */
    public List<List<Object>> getRowsAsLists(PathQuery query) {
        return getRows(query, 0, null).getRowsAsLists();
    }

    /**
     * Get results for a query as rows of objects. 
     *
     * @param query The query to run.
     * @param start The index of the first result to include.
     * @param maxCount The maximum size of the result set (or 10,000,000 if null)
     *
     * @return a list of rows, which are each a map from output colum (in alternate long and short form) to value.
     */
    public List<Map<String, Object>> getRowsAsMaps(String query, int start, Integer maxCount) {
        return getRows(query, start, maxCount).getRowsAsMaps();
    }

    /**
     * Get results for a query as rows of objects. 
     *
     * @param query The query to run.
     * @param start The index of the first result to include.
     * @param maxCount The maximum size of the result set (or 10,000,000 if null)
     *
     * @return a list of rows, which are each a map from output colum (in alternate long and short form) to value.
     */
    public List<Map<String, Object>> getRowsAsMaps(PathQuery query, int start, Integer maxCount) {
        return getRows(query, start, maxCount).getRowsAsMaps();
    }

    /**
     * Get results for a query as rows of objects. Get up to the maximum result size of 10,000,000 rows.
     *
     * @param query The query to run.
     * @param start The index of the first result to include.
     *
     * @return a list of rows, which are each a map from output colum (in alternate long and short form) to value.
     */
    public List<Map<String, Object>> getRowsAsMaps(String query, int start) {
        return getRows(query, start, null).getRowsAsMaps();
    }

    /**
     * Get results for a query as rows of objects. Get up to the maximum result size of 10,000,000 rows.
     *
     * @param query The query to run.
     * @param start The index of the first result to include.
     *
     * @return a list of rows, which are each a map from output colum (in alternate long and short form) to value.
     */
    public List<Map<String, Object>> getRowsAsMaps(PathQuery query, int start) {
        return getRows(query, start, null).getRowsAsMaps();
    }

    /**
     * Get results for a query as rows of objects. Get up to the maximum result size of 10,000,000 rows from the beginning.
     *
     * @param query The query to run.
     *
     * @return a list of rows, which are each a map from output colum (in alternate long and short form) to value.
     */
    public List<Map<String, Object>> getRowsAsMaps(String query) {
        return getRows(query, 0, null).getRowsAsMaps();
    }

    /**
     * Get results for a query as rows of objects. Get up to the maximum result size of 10,000,000 rows from the beginning.
     *
     * @param query The query to run.
     *
     * @return a list of rows, which are each a map from output colum (in alternate long and short form) to value.
     */
    public List<Map<String, Object>> getRowsAsMaps(PathQuery query) {
        return getRows(query, 0, null).getRowsAsMaps();
    }

    /**
     * Get an iterator over the results of a query. The iterator returns a representation 
     * of one row at a time, in the order received over the connection.
     *
     * @param query the query to run.
     * @param start the index of the first result to include.
     * @param maxCount The maximum size of the result set (or 10,000,000 if null).
     *
     * @return an iterator over the rows, where each row is a list of objects.
     */
    public Iterator<List<Object>> getRowListIterator(String query, int start, Integer maxCount) {
        return getRows(query, start, maxCount).getListIterator();
    }

    /**
     * Get an iterator over the results of a query. The iterator returns a representation 
     * of one row at a time, in the order received over the connection.
     *
     * @param query the query to run.
     * @param start the index of the first result to include.
     * @param maxCount The maximum size of the result set (or 10,000,000 if null).
     *
     * @return an iterator over the rows, where each row is a list of objects.
     */
    public Iterator<List<Object>> getRowListIterator(PathQuery query, int start, Integer maxCount) {
        return getRows(query, start, maxCount).getListIterator();
    }

    /**
     * Get an iterator over the results of a query. The iterator returns a representation 
     * of one row at a time, in the order received over the connection. Retrieves up to the maximum
     * result size of 10,000,000 rows from the given starting point.
     *
     * @param query the query to run.
     * @param start the index of the first result to include.
     *
     * @return an iterator over the rows, where each row is a list of objects.
     */
    public Iterator<List<Object>> getRowListIterator(String query, int start) {
        return getRows(query, start, null).getListIterator();
    }

    /**
     * Get an iterator over the results of a query. The iterator returns a representation 
     * of one row at a time, in the order received over the connection. Retrieves up to the maximum
     * result size of 10,000,000 rows from the given starting point.
     *
     * @param query the query to run.
     * @param start the index of the first result to include.
     *
     * @return an iterator over the rows, where each row is a list of objects.
     */
    public Iterator<List<Object>> getRowListIterator(PathQuery query, int start) {
        return getRows(query, start, null).getListIterator();
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
    public Iterator<List<Object>> getRowListIterator(String query) {
        return getRows(query, 0, null).getListIterator();
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
    public Iterator<List<Object>> getRowListIterator(PathQuery query) {
        return getRows(query, 0, null).getListIterator();
    }

    /**
     * Get an iterator over the results of a query. The iterator returns a representation 
     * of one row at a time, in the order received over the connection.
     *
     * @param query the query to run.
     * @param start the index of the first result to include.
     * @param maxCount The maximum size of the result set (or 10,000,000 if null).
     *
     * @return an iterator over the rows, where each row is a mapping from output column to value.
     */
    public Iterator<Map<String, Object>> getRowMapIterator(String query, int start, Integer maxCount) {
        return getRows(query, start, maxCount).getMapIterator();
    }

    /**
     * Get an iterator over the results of a query. The iterator returns a representation 
     * of one row at a time, in the order received over the connection.
     *
     * @param query the query to run.
     * @param start the index of the first result to include.
     * @param maxCount The maximum size of the result set (or 10,000,000 if null).
     *
     * @return an iterator over the rows, where each row is a mapping from output column to value.
     */
    public Iterator<Map<String, Object>> getRowMapIterator(PathQuery query, int start, Integer maxCount) {
        return getRows(query, start, maxCount).getMapIterator();
    }

    /**
     * Get an iterator over the results of a query. The iterator returns a representation 
     * of one row at a time, in the order received over the connection, up to the maximum result size
     * of 10,000,000 rows from the given starting point.
     *
     * @param query the query to run.
     * @param start the index of the first result to include.
     *
     * @return an iterator over the rows, where each row is a mapping from output column to value.
     */
    public Iterator<Map<String, Object>> getRowMapIterator(String query, int start) {
        return getRows(query, start, null).getMapIterator();
    }

    /**
     * Get an iterator over the results of a query. The iterator returns a representation 
     * of one row at a time, in the order received over the connection, up to the maximum result size
     * of 10,000,000 rows from the given starting point.
     *
     * @param query the query to run.
     * @param start the index of the first result to include.
     *
     * @return an iterator over the rows, where each row is a mapping from output column to value.
     */
    public Iterator<Map<String, Object>> getRowMapIterator(PathQuery query, int start) {
        return getRows(query, start, null).getMapIterator();
    }

    /**
     * Get an iterator over the results of a query. The iterator returns a representation 
     * of one row at a time, in the order received over the connection, up to the maximum result size
     * of 10,000,000 rows from the beginning.
     *
     * @param query the query to run.
     *
     * @return an iterator over the rows, where each row is a mapping from output column to value.
     */
    public Iterator<Map<String, Object>> getRowMapIterator(String query) {
        return getRows(query, 0, null).getMapIterator();
    }

    /**
     * Get an iterator over the results of a query. The iterator returns a representation 
     * of one row at a time, in the order received over the connection, up to the maximum result size
     * of 10,000,000 rows from the beginning.
     *
     * @param query the query to run.
     *
     * @return an iterator over the rows, where each row is a mapping from output column to value.
     */
    public Iterator<Map<String, Object>> getRowMapIterator(PathQuery query) {
        return getRows(query, 0, null).getMapIterator();
    }

    private RowResultSet getRows(String query, int start, Integer maxCount) {
        PathQuery pq = createPathQuery(query);
        return getRows(pq, start, maxCount);
    }

    private RowResultSet getRows(PathQuery query, int start, Integer maxCount) {
        List<String> views = query.getView();
        String queryXml = query.toXml(PathQuery.USERPROFILE_VERSION);
        QueryRequest request = new QueryRequest(RequestType.POST, getUrl(), ContentType.APPLICATION_JSON);
        if (maxCount != null) {
            request.setMaxCount(maxCount);
        }
        request.setStart(start);
        request.setQueryXml(queryXml);
        return getRows(request, views);
    }

    private RowResultSet getRows(QueryRequest request, List<String> views) {
        request.setJSONRowsFormat();
        HttpConnection connection = executeRequest(request);
        return new RowResultSet(connection, views);
    }

    /**
     * Performs the query and returns a JSONResult containing the data.
     * This is an internal method for handling the intermediate processing steps
     *
     * @param request a QueryRequest object
     *
     * @return a JSONResult object containing the data fetched
     */
    protected JSONResult getJSONResponse(QueryRequest request) {
        HttpConnection connection = executeRequest(request);
        return new JSONResult(connection);
    }

}
