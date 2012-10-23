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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.webservice.client.core.ContentType;
import org.intermine.webservice.client.core.Request.RequestType;
import org.intermine.webservice.client.core.RequestImpl;
import org.intermine.webservice.client.exceptions.ServiceException;
import org.intermine.webservice.client.results.JSONResult;
import org.intermine.webservice.client.results.Page;
import org.intermine.webservice.client.results.RowResultSet;
import org.intermine.webservice.client.results.XMLTableResult;
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
 *     ServiceFactory services = new ServiceFactory(serviceRootUrl);
 *     QueryService queryService = services.getQueryService();
 *
 *     PathQuery query = new PathQuery(services.getModel());
 *     query.addViews("Gene.symbol", "Gene.length", "Gene.proteins.primaryIdentifier");
 *     query.addConstraint(Constraints.lookup("Gene", "zen", "D. melanogaster"));
 *
 *     //find out how many results there are
 *     out.printf("There are %d results for this query\n", queryService.getCount(query));
 *
 *     Iterator<List<Object>> results = queryService.getRowListIterator(query);
 *
 *     while (results.hasNext()) {
 *         out.println(StringUtils.join(results.next(), "\t"));
 *     }
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
 * @author Alex Kalderimis
 * @author Jakub Kulaviak
 **/
public class QueryService extends AbstractQueryService<PathQuery>
{

    private static final String SERVICE_RELATIVE_URL = "query/results";

    private static final Set<String> NUMERIC_TYPES
        = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "int", "float", "double", "short", "long",
            "java.util.Integer", "java.util.Float", "java.util.Double",
            "java.util.Short", "java.util.Long", "java.util.BegDecimal")));

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

    @Override
    public int getCount(PathQuery query) {
        return getCount(query.toXml());
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
        return getIntResponse(request);
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
        return getJSONResults(queryXml, Page.DEFAULT);
    }

    @Override
    public List<JSONObject> getJSONResults(PathQuery query, Page page) throws JSONException {
        return getJSONResults(query.toXml(), page);
    }

    /**
     * Fetch some results as a list of JSON Objects. You define which results you want
     * by supplying a start index, and a maximum page size.
     *
     * @param queryXml An XML string representing the query
     * @param page The subsection of the result set to retrieve.
     *
     * @return a list of JSON objects
     *
     * @throws JSONException if the server returns badly formatted JSON, or non-JSON data
     */
    public List<JSONObject> getJSONResults(String queryXml, Page page)
        throws JSONException {
        QueryRequest request
            = new QueryRequest(RequestType.POST, getUrl(), ContentType.APPLICATION_JSON_OBJ);
        request.setQueryXml(queryXml);
        request.setPage(page);
        JSONResult response = getJSONResponse(request);
        return response.getObjects();
    }

    @Override
    public List<List<String>> getResults(PathQuery query, Page page) {
        return getResultInternal(query.toXml(), page).getData();
    }

    /**
     * Returns a specific set of the results of the specified PathQuery.
     * If you expect a lot of results, or have set a very large page size
     * we would recommend that you use the getResultIterator() method instead, as that will
     * read through the lines one by one without storing them all in memory at once.
     *
     * @param queryXml PathQuery represented as a XML string""
     * @param page The subsection of the result set to retrieve.
     * @return results of specified PathQuery
     */
    public List<List<String>> getResults(String queryXml, Page page) {
        return getResultInternal(queryXml, page).getData();
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
        return getResultInternal(queryXml, Page.DEFAULT).getData();
    }

    @Override
    public Iterator<List<String>> getRowIterator(PathQuery query, Page page) {
        return getResultInternal(query.toXml(), page).getIterator();
    }

    /**
     * Returns a specific set of the results of a specified PathQuery as an iterator.
     * We would recommend that you use this method if you expect a lot of rows of results and
     * might run out of memory.
     *
     * @param queryXml the query represented as an XML string
     * @param page The subsection of the result set to retrieve.
     * @return results of specified PathQuery
     */
    public Iterator<List<String>> getRowIterator(String queryXml, Page page) {
        return getResultInternal(queryXml, page).getIterator();
    }

    /**
     * Returns all the results of a specified PathQuery as an iterator. We would recommend that
     * you use this method if you expect a lot of rows of results and might run out of memory.
     *
     * @param queryXml the query represented as an XML string
     *
     * @return an iterator over the results of the specified PathQuery
     */
    public Iterator<List<String>> getAllRowIterator(String queryXml) {
        return getRowIterator(queryXml, Page.DEFAULT);
    }

    /**
     * Prepare the request by setting the appropriate parameters onto the request object, before
     * sending it off to receive the results.
     *
     * @param queryXml the query represented as an XML string
     * @param page The subsection of the result set to retrieve.
     * @return a data table object
     */
    private XMLTableResult getResultInternal(String queryXml, Page page) {
        QueryRequest request = new QueryRequest(RequestType.POST, getUrl(),
                ContentType.TEXT_XML);
        request.setPage(page);
        request.setQueryXml(queryXml);
        return getResponseTable(request);
    }

    /**
     * Get results for a query as rows of objects.
     *
     * @param query The query to run.
     * @param page The subsection of the result set to retrieve.
     * @return a list of rows, which are each a list of cells.
     */
    public List<List<Object>> getRowsAsLists(String query, Page page) {
        return getRows(query, page).getRowsAsLists();
    }

    /**
     * Get results for a query as rows of objects. Retrieve up to
     * 10,000,000 rows from the beginning.
     *
     * @param query The query to run.
     *
     * @return a list of rows, which are each a list of cells.
     */
    public List<List<Object>> getRowsAsLists(String query) {
        return getRows(query, Page.DEFAULT).getRowsAsLists();
    }

    /**
     * Get results for a query as rows of objects.
     *
     * @param query The query to run.
     * @param page The subsection of the result set to retrieve.
     * @return a list of rows, which are each a map from output column
     * (in alternate long and short form) to value.
     */
    public List<Map<String, Object>> getRowsAsMaps(String query, Page page) {
        return getRows(query, page).getRowsAsMaps();
    }

    /**
     * Get results for a query as rows of objects.
     * Get up to the maximum result size of 10,000,000 rows from the beginning.
     *
     * @param query The query to run.
     *
     * @return a list of rows, which are each a map from
     * output column (in alternate long and short form) to value.
     */
    public List<Map<String, Object>> getRowsAsMaps(String query) {
        return getRows(query, Page.DEFAULT).getRowsAsMaps();
    }

    /**
     * Get an iterator over the results of a query. The iterator returns a representation
     * of one row at a time, in the order received over the connection.
     *
     * @param query the query to run.
     * @param page The subsection of the result set to retrieve.
     * @return an iterator over the rows, where each row is a list of objects.
     */
    public Iterator<List<Object>> getRowListIterator(String query, Page page) {
        return getRows(query, page).getListIterator();
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
        return getRows(query, Page.DEFAULT).getListIterator();
    }

    /**
     * Get an iterator over the results of a query. The iterator returns a representation
     * of one row at a time, in the order received over the connection.
     *
     * @param query the query to run.
     * @param page The subsection of the result set to retrieve.
     * @return an iterator over the rows, where each row is a mapping from output column to value.
     */
    public Iterator<Map<String, Object>> getRowMapIterator(String query, Page page) {
        return getRows(query, page).getMapIterator();
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
    public Iterator<Map<String, Object>> getRowMapIterator(String query) {
        return getRows(query, Page.DEFAULT).getMapIterator();
    }

    private RowResultSet getRows(String query, Page page) {
        PathQuery pq = createPathQuery(query);
        return getRows(pq, page);
    }

    @Override
    protected RowResultSet getRows(PathQuery query, Page page) {
        List<String> views = query.getView();
        String queryXml = query.toXml(PathQuery.USERPROFILE_VERSION);
        ContentType ct = (this.getAPIVersion() < 8)
                ? ContentType.APPLICATION_JSON_ROW : ContentType.APPLICATION_JSON;
        QueryRequest request =
                new QueryRequest(RequestType.POST, getUrl(), ct);
        request.setPage(page);
        request.setQueryXml(queryXml);
        return getRows(request, views);
    }

    /**
     * Get a summary for the values in column of a query.
     *
     * The column must represent a column of numeric values.
     *
     * @param query The query to summarise.
     * @param summaryPath The column to summarise.
     * @return A summary.
     */
    public NumericSummary getNumericSummary(PathQuery query, String summaryPath) {
        try {
            if (!summaryPath.startsWith(query.getRootClass())) {
                summaryPath = query.getRootClass() + "." + summaryPath;
            }
        } catch (PathException e) {
            throw new ServiceException("Error with query", e);
        }
        Path p = null;
        try {
            p = query.makePath(summaryPath);
        } catch (PathException e) {
            throw new ServiceException("while requesting numeric summary information", e);
        }
        FieldDescriptor fd = p.getEndFieldDescriptor();
        if (!fd.isAttribute()) {
            throw new ServiceException(summaryPath + " does not describe an attribute");
        } else {
            String dataType = ((AttributeDescriptor) fd).getType();
            if (!NUMERIC_TYPES.contains(dataType)) {
                throw new ServiceException(summaryPath + " does not represent a numeric column");
            }
        }
        QueryRequest request = new QueryRequest(RequestType.POST, getUrl(),
                ContentType.APPLICATION_JSON_ROW);
        request.setQueryXml(query.toXml());
        request.setParameter("summaryPath", summaryPath);
        JSONResult response = getJSONResponse(request);
        try {
            return new NumericSummary(summaryPath, response.getObjects().get(0));
        } catch (JSONException e) {
            throw new ServiceException("Error parsing JSON response", e);
        }
    }

    /**
     * Get a summary for the values in column of a query.
     *
     * The column must represent a column of non-numeric values. The map returned represents
     * each possible value as a key, with the count of the occurrences of that value as the
     * associated value for that key. The map supports predictable iteration ordering from
     * the most request value to the least.
     *
     * @param query The query to summarise.
     * @param summaryPath The column to summarise.
     * @return A summary.
     */
    public Map<String, Integer> getSummary(PathQuery query, String summaryPath) {
        return getSummary(query, summaryPath, Page.DEFAULT);
    }

    /**
     * Get a summary for the values in column of a query.
     *
     * The column must represent a column of non-numeric values. The map returned represents
     * each possible value as a key, with the count of the occurrences of that value as the
     * associated value for that key. The map supports predictable iteration ordering from
     * the most request value to the least.
     *
     * @param query The query to summarise.
     * @param summaryPath The column to summarise.
     * @param page The subsection of the summary to retrieve.
     * @return A summary.
     */
    public Map<String, Integer> getSummary(PathQuery query, String summaryPath, Page page) {
        try {
            if (!summaryPath.startsWith(query.getRootClass())) {
                summaryPath = query.getRootClass() + "." + summaryPath;
            }
        } catch (PathException e) {
            throw new ServiceException("Error with query", e);
        }
        Path p = null;
        try {
            p = query.makePath(summaryPath);
        } catch (PathException e) {
            throw new ServiceException("while requesting numeric summary information", e);
        }
        FieldDescriptor fd = p.getEndFieldDescriptor();
        if (!fd.isAttribute()) {
            throw new ServiceException(summaryPath + " does not describe an attribute");
        } else {
            String dataType = ((AttributeDescriptor) fd).getType();
            if (NUMERIC_TYPES.contains(dataType)) {
                throw new ServiceException(summaryPath + " represents a numeric column");
            }
        }
        QueryRequest request = new QueryRequest(RequestType.POST, getUrl(),
                ContentType.APPLICATION_JSON_ROW);
        request.setQueryXml(query.toXml());
        request.setPage(page);
        request.setParameter("summaryPath", summaryPath);
        JSONResult response = getJSONResponse(request);
        Map<String, Integer> ret = new LinkedHashMap<String, Integer>();
        try {
            Iterator<JSONObject> it = response.getIterator();
            while (it.hasNext()) {
                JSONObject record = it.next();
                ret.put(record.getString("item"), record.getInt("count"));
            }
        } catch (JSONException e) {
            throw new ServiceException("Error parsing JSON response", e);
        }
        return ret;
    }

    /**
     * Result format for Numeric Summary information.
     * @author Alex Kalderimis
     */
    public static final class NumericSummary
    {

        private final String column;
        private final double average;
        private final double max;
        private final double min;
        private final double standardDeviation;

        private NumericSummary(String name, JSONObject data) throws JSONException {
            column = name;
            average = data.getDouble("average");
            max = data.getDouble("max");
            min = data.getDouble("min");
            standardDeviation = data.getDouble("stdev");
        }

        /** @return The name of the column this summary is about. **/
        public String getColumn() {
            return column;
        }

        /** @return the average of the numeric values in this column **/
        public double getAverage() {
            return average;
        }


        /** @return the maximum value in this column **/
        public double getMax() {
            return max;
        }

        /** @return the minimum value in this column **/
        public double getMin() {
            return min;
        }

        /** @return the standard deviation of the values in this column **/
        public double getStandardDeviation() {
            return standardDeviation;
        }
    }


}
