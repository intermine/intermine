package org.intermine.webservice.server.query.result;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.EnumerationUtils;
import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Results;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.WebUtil;
import org.intermine.webservice.server.ColumnHeaderStyle;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.WebServiceInput;
import org.intermine.webservice.server.WebServiceRequestParser;
import org.intermine.webservice.server.core.CountProcessor;
import org.intermine.webservice.server.core.ResultProcessor;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.output.FlatFileFormatter;
import org.intermine.webservice.server.output.JSONObjResultProcessor;
import org.intermine.webservice.server.output.JSONResultFormatter;
import org.intermine.webservice.server.output.JSONRowResultProcessor;
import org.intermine.webservice.server.output.JSONSummaryProcessor;
import org.intermine.webservice.server.output.JSONTableFormatter;
import org.intermine.webservice.server.output.JSONTableResultProcessor;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.ResultsIterator;
import org.intermine.webservice.server.output.StreamedOutput;
import org.intermine.webservice.server.query.AbstractQueryService;

/**
 * Executes query and returns results. Other parameters in request can specify
 * range of returned results, format ... For using of web service and parameter
 * description see InterMine wiki pages. 1) Validates parameters and tries
 * validate xml query as much as possible. Validates xml query according to XML
 * Schema and and finds out if there were some errors during unmarshalling
 * PathQuery from xml. 2) Executes created PathQuery. 3) Print results to
 * output.
 *
 * @author Jakub Kulaviak
 * @author Alex Kalderimis
 */

public class QueryResultService extends AbstractQueryService
{

    /** Batch size to use **/
    public static final int BATCH_SIZE = 5000;
    protected Map<String, Object> attributes = new HashMap<String, Object>();

    /**
     * Constructor
     * @param im The InterMineAPI settings bundle for this webservice
     */
    public QueryResultService(InterMineAPI im) {
        super(im);
    }

    /**
     * Executes service specific logic.
     */
    @Override
    protected void execute() {
        QueryResultInput input = getInput();
        PathQueryBuilder builder = getQueryBuilder(input.getXml());
        PathQuery query = builder.getQuery();
        setHeaderAttributes(query, input.getStart(), input.getMaxCount());
        runPathQuery(query, input.getStart(), input.getMaxCount(), null, null,
                input, null, input.getLayout());
    }

    @Override
    protected int getDefaultFormat() {
        return TSV_FORMAT;
    }

    /**
     * Returns the path portion of a link to the results for
     * this query in its originating mine, in the given format.
     * @param pq The PathQuery
     * @param format The desired format
     * @return The path portion of the link.
     */
    protected String getLinkPath(PathQuery pq, String format) {
        QueryResultLinkGenerator linkGen = new QueryResultLinkGenerator();
        String xml = pq.toXml(PathQuery.USERPROFILE_VERSION);
        return linkGen.getLinkPath(xml, format);
    }

    /**
     * Returns the path portion of the link to the results for this query in
     * its originating mine.
     * @param pq The query
     * @return The path section of the link
     */
    protected String getMineResultsLinkPath(PathQuery pq) {
        QueryResultLinkGenerator linkGen = new QueryResultLinkGenerator();
        String xml = pq.toXml(PathQuery.USERPROFILE_VERSION);
        return linkGen.getMineResultsPath(xml);
    }
    
    /**
     * Set the header attributes of the output based on the values of the PathQuery
     *
     * @param pq The path query to be run
     * @param start The beginning of this set of results
     * @param size The size of this set of results
     */
    protected void setHeaderAttributes(PathQuery pq, Integer start, Integer size) {
        
        if (formatIsJSON()) {
            // These attributes are always needed
            attributes.put(JSONResultFormatter.KEY_MODEL_NAME, pq.getModel().getName());
            attributes.put(JSONResultFormatter.KEY_VIEWS, pq.getView());

            attributes.put(JSONTableFormatter.KEY_COLUMN_HEADERS,
                    WebUtil.formatPathQueryView(pq, request));
            attributes.put("start", String.valueOf(start));
            try {
                attributes.put(JSONResultFormatter.KEY_ROOT_CLASS, pq.getRootClass());
            } catch (PathException e) {
                throw new RuntimeException(e);
            }
            String summaryPath = request.getParameter("summaryPath");
            if (!isBlank(summaryPath)) {
                PathQueryExecutor executor = getPathQueryExecutor();
                int count;
                try {
                    count = executor.uniqueColumnValues(pq, summaryPath);
                } catch (ObjectStoreException e) {
                    throw new ServiceException("Problem getting unique column value count.", e);
                }
                attributes.put("uniqueValues", count);
            }
        }
        int f = getFormat();
        if (f == WebService.JSON_TABLE_FORMAT || f == WebService.JSONP_TABLE_FORMAT) {
            String csvUrl = getLinkPath(pq, WebServiceRequestParser.FORMAT_PARAMETER_CSV);
            String tsvUrl =  getLinkPath(pq, WebServiceRequestParser.FORMAT_PARAMETER_TAB);
            String pageUrl = getLinkPath(pq, WebServiceRequestParser.FORMAT_PARAMETER_JSONP_ROW);
            String countUrl = getLinkPath(pq, WebServiceRequestParser.FORMAT_PARAMETER_JSONP_COUNT);
            String mineResLink = getMineResultsLinkPath(pq);
            List<String> viewTypes = new ArrayList<String>();
            for (String v: pq.getView()) {
                try {
                    Path p = pq.makePath(v);
                    AttributeDescriptor ad = (AttributeDescriptor) p.getEndFieldDescriptor();
                    viewTypes.add(ad.getType());
                } catch (PathException e) {
                    throw new ServiceException(e);
                }
            }
            String title = pq.getTitle();
            String description;
            if (pq.getDescription() == null) {
                description = pq.toString();
            } else {
                description = pq.getDescription();
            }
            attributes.put("viewTypes", viewTypes);
            attributes.put("size", String.valueOf(size));
            attributes.put("pagePath", pageUrl);
            attributes.put("mineResultsLink", mineResLink);
            attributes.put(JSONTableFormatter.KEY_CURRENT_PAGE, pageUrl);
            attributes.put(JSONTableFormatter.KEY_EXPORT_CSV_URL, csvUrl);
            attributes.put(JSONTableFormatter.KEY_EXPORT_TSV_URL, tsvUrl);
            attributes.put(JSONTableFormatter.KEY_TITLE, title);
            attributes.put(JSONTableFormatter.KEY_DESCRIPTION, description);
            attributes.put(JSONTableFormatter.KEY_COUNT, countUrl);
        }
        if (f == JSON_DATA_TABLE_FORMAT || f == JSONP_DATA_TABLE_FORMAT) {
            attributes.put("sEcho", request.getParameter("sEcho"));
            attributes.put("sColumns", StringUtils.join(pq.getView(), ","));
            //attributes.put("sColumns", StringUtils.join(
            //        WebUtil.formatPathQueryView(pq, request), ","));
            PathQueryExecutor executor = getPathQueryExecutor();
            int count;
            try {
                count = executor.count(pq);
            } catch (ObjectStoreException e) {
                throw new ServiceException("Problem getting count.", e);
            }
            attributes.put("iTotalRecords", count);
            attributes.put("iTotalDisplayRecords", count);
        }
        if (formatIsJSONP()) {
            String callback = getCallback();
            if (callback == null) {
                callback = "makeInterMineResultsTable";
            }
            attributes.put(JSONResultFormatter.KEY_CALLBACK, callback);
        }
        if (formatIsFlatFile()) {
            if (wantsColumnHeaders()) {
                if (ColumnHeaderStyle.FRIENDLY == getColumnHeaderStyle()) {
                    attributes.put(FlatFileFormatter.COLUMN_HEADERS,
                            WebUtil.formatPathQueryView(pq, request));
                } else {
                    attributes.put(FlatFileFormatter.COLUMN_HEADERS, pq.getView());
                }
            }
        }

        if (!isBlank(request.getParameter("summaryPath"))) {
            String summaryPath = request.getParameter("summaryPath");
            Path p;
            try {
                p = pq.makePath(summaryPath);
            } catch (PathException e) {
                throw new BadRequestException("Summary path is invalid");
            }
            if (!p.endIsAttribute()) {
                throw new BadRequestException("Summary path is invalid");
            }
            AttributeDescriptor ad = (AttributeDescriptor) p.getEndFieldDescriptor();
            String type = ad.getType();
            List<String> colHeaders = new ArrayList<String>();
            if ("int".equals(type) || "Integer".equals(type) || "Float".equals(type)
                    || "float".equals(type) || "Double".equals(type)
                    || "double".equals(type) || "long".equals(type)
                    || "Long".equals(type) || "Math.BigDecimal".equals(type)) {
                colHeaders.addAll(Arrays.asList("min", "max", "average", "standard-dev"));
            } else {
                colHeaders.addAll(Arrays.asList("item", "count"));
            }
            if (formatIsJSON()) {
                attributes.put(JSONTableFormatter.KEY_COLUMN_HEADERS, colHeaders);
            } else {
                if (formatIsFlatFile() && wantsColumnHeaders()) {
                    attributes.put(FlatFileFormatter.COLUMN_HEADERS, colHeaders);
                }
            }
        }

        output.setHeaderAttributes(attributes);
    }

    private String createBaseLink() {
        String baseLink = request.getRequestURL().toString() + "?";
        List<String> names = EnumerationUtils.toList(request.getParameterNames());
        while (names.contains(WebServiceRequestParser.START_PARAMETER)) {
            names.remove(WebServiceRequestParser.START_PARAMETER);
        }
        while (names.contains(WebServiceRequestParser.LIMIT_PARAMETER)) {
            names.remove(WebServiceRequestParser.LIMIT_PARAMETER);
        }
        boolean firstParameter = true;
        for (String name : names) {
            String[] values = request.getParameterValues(name);
            for (String value : values) {
                if (firstParameter) {
                    // don't place ampersand
                    firstParameter = false;
                } else {
                    baseLink += "&";
                }
                baseLink += name + "=" + encode(value);
            }
        }
        return baseLink;
    }

    @Override
    protected Output makeJSONOutput(PrintWriter out, String separator) {
        return new StreamedOutput(out, new JSONTableFormatter(), separator);
    }

    /**
     * URL Encode an object. Null values are returned as the empty string, and encoding problems
     * throw runtime exceptions.
     * @param o The thing to encode.
     * @return The encoded version.
     */
    protected static String encode(Object o) {
        if (o == null) {
            return "";
        } else {
            try {
                return URLEncoder.encode(o.toString(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Encoding string failed: "
                        + o.toString(), e);
            }
        }
    }


    /**
     * Runs path query and returns to output obtained results.
     *
     * @param pathQuery
     *            path query
     * @param firstResult
     *            index of first result, that should be returned
     * @param maxResults
     *            maximum number of results
     * @param title
     *            title displayed in html output, can be null
     * @param description
     *            description displayed in html output, can be null
     * @param input
     *            input of web service
     * @param mineLink
     *            link pointing results of this query (template) in InterMine,
     *            can be null
     * @param layout
     *            results table layout string, can be null
     */
    public void runPathQuery(PathQuery pathQuery, int firstResult,
            int maxResults, String title, String description,
            WebServiceInput input, String mineLink, String layout) {
        PathQueryExecutor executor = getPathQueryExecutor();

        if (formatIsCount()) {
            int count;
            try {
                count = executor.count(pathQuery);
            } catch (ObjectStoreException e) {
                throw new ServiceException("Problem getting count.", e);
            }
            CountProcessor processor = new CountProcessor();
            processor.writeCount(count, output);
        } else {
            boolean canGoFaster = false;
            Iterator<List<ResultElement>> it;
            String summaryPath = request.getParameter("summaryPath");
            if (!StringUtils.isBlank(summaryPath)) {
                try {
                    String filterTerm = request.getParameter("filterTerm");
                    Results r = executor.summariseQuery(pathQuery, summaryPath, filterTerm);
                    if (StringUtils.isNotBlank(filterTerm)) {
                        attributes.put("filteredCount", r.size());
                    }
                    it = new ResultsIterator(r, firstResult, maxResults, filterTerm);
                } catch (ObjectStoreException e) {
                    throw new ServiceException("Problem getting summary.", e);
                }
            } else {
                canGoFaster = true;
                executor.setBatchSize(BATCH_SIZE);
                it = executor.execute(pathQuery, firstResult, maxResults);
            }

            ResultProcessor processor = makeResultProcessor();
            if (it.hasNext()) { // Prime the batch fetching pumps
                try {
                    if (canGoFaster) {
                        ((ExportResultsIterator) it).goFaster();
                    }
                    processor.write(it, output);
                } finally {
                    if (canGoFaster) {
                        ((ExportResultsIterator) it).releaseGoFaster();
                    }
                }
            }
        }
    }

    private ResultProcessor makeResultProcessor() {
        ResultProcessor processor;
        boolean summarising = !StringUtils.isBlank(request.getParameter("summaryPath"));
        switch(getFormat()) {
            case WebService.JSON_FORMAT:
                processor
                    = new JSONRowResultProcessor(im, JSONRowResultProcessor.Verbosity.MINIMAL);
                break;
            case WebService.JSONP_FORMAT:
                processor
                    = new JSONRowResultProcessor(im, JSONRowResultProcessor.Verbosity.MINIMAL);
                break;
            case WebService.JSON_OBJ_FORMAT:
                processor = new JSONObjResultProcessor();
                break;
            case WebService.JSONP_OBJ_FORMAT:
                processor = new JSONObjResultProcessor();
                break;
            case WebService.JSON_TABLE_FORMAT:
                processor = new JSONTableResultProcessor();
                break;
            case WebService.JSONP_TABLE_FORMAT:
                processor = new JSONTableResultProcessor();
                break;
            case WebService.JSON_ROW_FORMAT:
                if (summarising) {
                    processor = new JSONSummaryProcessor();
                } else {
                    processor = new JSONRowResultProcessor(im);
                }
                break;
            case WebService.JSONP_ROW_FORMAT:
                if (summarising) {
                    processor = new JSONSummaryProcessor();
                } else {
                    processor = new JSONRowResultProcessor(im);
                }
                break;
            case WebService.JSON_DATA_TABLE_FORMAT:
                processor = new JSONRowResultProcessor(im);
                break;
            case WebService.JSONP_DATA_TABLE_FORMAT:
                processor = new JSONRowResultProcessor(im);
                break;
            default:
                processor = new ResultProcessor();
        }
        return processor;
    }

    private PathQueryExecutor getPathQueryExecutor() {
        final Profile profile = permission.getProfile();
        final PathQueryExecutor executor = im.getPathQueryExecutor(profile);
        return executor;
    }


    private QueryResultInput getInput() {
        return new QueryResultRequestParser(request).getInput();
    }
}
