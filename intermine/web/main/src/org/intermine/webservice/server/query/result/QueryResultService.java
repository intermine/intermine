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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections.EnumerationUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.InterMineAction;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.WebServiceInput;
import org.intermine.webservice.server.core.CountProcessor;
import org.intermine.webservice.server.core.ResultProcessor;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.output.JSONObjResultProcessor;
import org.intermine.webservice.server.output.JSONResultFormatter;
import org.intermine.webservice.server.output.JSONRowResultProcessor;
import org.intermine.webservice.server.output.JSONTableFormatter;
import org.intermine.webservice.server.output.JSONTableResultProcessor;
import org.intermine.webservice.server.output.MemoryOutput;
import org.jfree.util.Log;
import org.json.JSONArray;

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
 */

public class QueryResultService extends WebService
{

    private static final String XML_SCHEMA_LOCATION = "webservice/query.xsd";

    private static final int BATCH_SIZE = 5000;

    /**
     * Constructor
     * @param im The InterMineAPI settings bundle for this webservice
     */
    public QueryResultService(InterMineAPI im) {
        super(im);
    }

    /**
     * Executes service specific logic.
     *
     * @param request request
     * @param response response
     */
    @Override
    protected void execute(HttpServletRequest request,
            HttpServletResponse response) {

        QueryResultInput input = getInput();

        PathQueryBuilder builder = getQueryBuilder(input.getXml(), request);

        PathQuery query = builder.getQuery();
        setHeaderAttributes(query, input.getStart(), input.getMaxCount(), null);
        runPathQuery(query, input.getStart(), input.getMaxCount(), null, null,
                input, null, input.getLayout());
    }

    private PathQueryBuilder getQueryBuilder(String xml, HttpServletRequest req) {
        HttpSession session = req.getSession();
        Profile profile = SessionMethods.getProfile(session);
        BagManager bagManager = this.im.getBagManager();

        Map<String, InterMineBag> savedBags = bagManager
                .getUserAndGlobalBags(profile);

        if (getFormat() == WebService.JSON_OBJ_FORMAT) {
            return new PathQueryBuilderForJSONObj(xml, getXMLSchemaUrl(),
                    savedBags);
        } else {
            return new PathQueryBuilder(xml, getXMLSchemaUrl(), savedBags);
        }
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
     * @param title The title of this query
     * @param description A description of this query
     */
    protected void setHeaderAttributes(PathQuery pq, Integer start, Integer size,
            String title) {
        Map<String, String> attributes = new HashMap<String, String>();
        if (formatIsJSON()) {
            // These attributes are always needed
            attributes.put(JSONResultFormatter.KEY_MODEL_NAME, pq.getModel().getName());
            attributes.put(JSONResultFormatter.KEY_VIEWS, new JSONArray(pq.getView()).toString());
            attributes.put("start", String.valueOf(start));
            try {
                attributes.put(JSONResultFormatter.KEY_ROOT_CLASS, pq.getRootClass());
            } catch (PathException e) {
                throw new RuntimeException("Cannot get root class name", e);
            }
        }
        int f = getFormat();
        if (f == WebService.JSON_TABLE_FORMAT || f == WebService.JSONP_TABLE_FORMAT) {
            List<String> columnNames = new ArrayList<String>();
            for (String viewString : pq.getView()) {
                columnNames.add("'" + pq.getGeneratedPathDescription(viewString) + "'");
            }
            String csvUrl = getLinkPath(pq, WebServiceRequestParser.FORMAT_PARAMETER_CSV);
            String tsvUrl =  getLinkPath(pq, WebServiceRequestParser.FORMAT_PARAMETER_TAB);
            String pageUrl = getLinkPath(pq, WebServiceRequestParser.FORMAT_PARAMETER_JSONP_ROW);
            String countUrl = getLinkPath(pq, WebServiceRequestParser.FORMAT_PARAMETER_JSONP_COUNT);
            String mineResLink = getMineResultsLinkPath(pq);

            if (title == null) {
                title = "Query Results";
            }
            String description;
            if (pq.getDescription() == null) {
                description = pq.toString();
            } else {
                description = pq.getDescription();
            }
            Log.info("base url is: " + pageUrl);
            attributes.put("size", String.valueOf(size));
            attributes.put("pagePath", pageUrl);
            attributes.put("mineResultsLink", mineResLink);
            attributes.put(JSONTableFormatter.KEY_COLUMN_HEADERS, columnNames.toString());
            attributes.put(JSONTableFormatter.KEY_CURRENT_PAGE, pageUrl);
            attributes.put(JSONTableFormatter.KEY_EXPORT_CSV_URL, csvUrl);
            attributes.put(JSONTableFormatter.KEY_EXPORT_TSV_URL, tsvUrl);
            attributes.put(JSONTableFormatter.KEY_TITLE, title);
            attributes.put(JSONTableFormatter.KEY_DESCRIPTION, description);
            attributes.put(JSONTableFormatter.KEY_COUNT, countUrl);
        }
        if (formatIsJSONP()) {
            String callback = getCallback();
            if (callback == null) {
                callback = "makeInterMineResultsTable";
            }
            attributes.put(JSONResultFormatter.KEY_CALLBACK, callback);
        }
        output.setHeaderAttributes(attributes);
    }

    private void forward(PathQuery pathQuery, String title, String description,
            WebServiceInput input, String mineLink, String layout) {
        if (getFormat() == WebService.HTML_FORMAT) {
            List<String> columnNames = new ArrayList<String>();
            for (String viewString : pathQuery.getView()) {
                columnNames.add(pathQuery.getGeneratedPathDescription(viewString));
            }
            MemoryOutput mout = (MemoryOutput) output;
            request.setAttribute("columnNames", columnNames);
            request.setAttribute("rows", mout.getResults());
            request.setAttribute("title", title);
            request.setAttribute("description", description);
            request.setAttribute("currentPage",
                    (input.getStart()) / input.getMaxCount());
            request.setAttribute("baseLink", createBaseLink());
            request.setAttribute("pageSize", input.getMaxCount());
            request.setAttribute("layout", layout);
            if (mineLink != null) {
                request.setAttribute("mineLinkText", "Results in "
                        + InterMineAction.getWebProperties(request)
                                .getProperty("project.title"));
                request.setAttribute("mineLinkUrl", mineLink);
            }
            try {
                getHtmlForward().forward(request, response);
            } catch (Exception e) {
                throw new InternalErrorException(e);
            }
        }
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
            executor.setBatchSize(BATCH_SIZE);
            ExportResultsIterator resultIt = executor.execute(pathQuery, firstResult, maxResults);
            ResultProcessor processor = makeResultProcessor();
            try {
                resultIt.goFaster();
                processor.write(resultIt, output);
            } finally {
                resultIt.releaseGoFaster();
            }
        }
        forward(pathQuery, title, description, input, mineLink, layout);
    }

    private ResultProcessor makeResultProcessor() {
        ResultProcessor processor;
        switch(getFormat()) {
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
                processor = new JSONRowResultProcessor(im);
                break;
            case WebService.JSONP_ROW_FORMAT:
                processor = new JSONRowResultProcessor(im);
                break;
            default:
                processor = new ResultProcessor();
        }
        return processor;
    }

    private PathQueryExecutor getPathQueryExecutor() {
        Profile profile = SessionMethods.getProfile(request.getSession());
        PathQueryExecutor executor = this.im.getPathQueryExecutor(profile);
        return executor;
    }

    private String getXMLSchemaUrl() {
        try {
            String relPath = request.getContextPath() + "/"
                    + XML_SCHEMA_LOCATION;
            URL url = new URL(request.getScheme(), request.getServerName(),
                    request.getServerPort(), relPath);
            return url.toString();
        } catch (MalformedURLException e) {
            throw new InternalErrorException(e);
        }
    }

    private QueryResultInput getInput() {
        return new QueryResultRequestParser(request).getInput();
    }
}
