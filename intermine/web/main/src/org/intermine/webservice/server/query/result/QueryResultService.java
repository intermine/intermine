package org.intermine.webservice.server.query.result;

/*
 * Copyright (C) 2002-2010 FlyMine
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
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.InterMineAction;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.WebServiceInput;
import org.intermine.webservice.server.core.ResultProcessor;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.intermine.webservice.server.output.JSONObjResultProcessor;
import org.intermine.webservice.server.output.MemoryOutput;
import org.json.JSONArray;

/**
 * Executes query and returns results. Other parameters in request can specify
 * range of returned results, format ...
 * For using of web service and parameter description see InterMine wiki pages.
 * 1) Validates parameters and tries validate xml query as much as possible. Validates xml
 * query according to XML Schema and and finds out if there were some errors during unmarshalling
 * PathQuery from xml.
 * 2) Executes created PathQuery.
 * 3) Print results to output.
 * @author Jakub Kulaviak
 */

public class QueryResultService extends WebService
{

    private static final String XML_SCHEMA_LOCATION = "webservice/query.xsd";

    private static final int BATCH_SIZE = 5000;

    public QueryResultService(InterMineAPI im) {
        super(im);
    }

    /**
     * Executes service specific logic.
     * @param request request
     * @param response response
     */
    @Override
    protected void execute(HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) {

        QueryResultInput input = getInput();
                
        PathQueryBuilder builder = getQueryBuilder(input.getXml(), request); 

        PathQuery query = builder.getQuery();
        setHeaderAttributes(query);
        runPathQuery(query, input.getStart(), input.getMaxCount(), null, null, input, null, input
                .getLayout());
    }
    
    private PathQueryBuilder getQueryBuilder(String xml, HttpServletRequest req) {
    	HttpSession session = req.getSession();
        Profile profile = SessionMethods.getProfile(session);
        BagManager bagManager = this.im.getBagManager();

        Map<String, InterMineBag> savedBags 
        	= bagManager.getUserAndGlobalBags(profile);
        
        if (getFormat() == WebService.JSON_OBJ_FORMAT) {
        	return new PathQueryBuilderForJSONObj(xml, getXMLSchemaUrl(), savedBags);
        } else {
        	return new PathQueryBuilder(xml, getXMLSchemaUrl(), savedBags);
        }
    }
    
    
    private void setHeaderAttributes(PathQuery pq) {
    	if (getFormat() == WebService.JSON_OBJ_FORMAT) {
	    	Map<String, String> attributes = new HashMap<String, String>();
	    	attributes.put("views", new JSONArray(pq.getView()).toString());
	    	try {
				attributes.put("rootClass", pq.getRootClass());
			} catch (PathException e) {
				throw new RuntimeException("Cannot get root class name", e);
			}
	    	
	    	output.setHeaderAttributes(attributes);
    	}
    }

    private void forward(PathQuery pathQuery, String title, String description,
            WebServiceInput input, String mineLink, String layout) {
        List<String> columnNames = new ArrayList<String>();
        for (String viewString : pathQuery.getView()) {
            columnNames.add(pathQuery.getGeneratedPathDescription(viewString));
        }
        if (getFormat() == WebService.HTML_FORMAT) {
            MemoryOutput mout = (MemoryOutput) output;
            request.setAttribute("columnNames", columnNames);
            request.setAttribute("rows", mout.getResults());
            request.setAttribute("title", title);
            request.setAttribute("description", description);
            request.setAttribute("currentPage", (input.getStart()) / input.getMaxCount());
            request.setAttribute("baseLink", createBaseLink());
            request.setAttribute("pageSize", input.getMaxCount());
            request.setAttribute("layout", layout);
            if (mineLink != null) {
                request.setAttribute("mineLinkText", "Results in "
                        + InterMineAction.getWebProperties(request).getProperty("project.title"));
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
        List<String> names =  EnumerationUtils.toList(request.getParameterNames());
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

    private static String encode(Object o) {
        if (o == null) {
            return "";
        } else {
            try {
                return URLEncoder.encode(o.toString(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Encoding string failed: " + o.toString(), e);
            }
        }
    }


    /**
     * Runs path query and returns to output obtained results.
     * @param pathQuery path query
     * @param firstResult index of first result, that should be returned
     * @param maxResults maximum number of results
     * @param title title displayed in html output, can be null
     * @param description description displayed in html output, can be null
     * @param input input of web service
     * @param mineLink link pointing results of this query (template) in InterMine, can be null
     * @param layout results table layout string, can be null
     */
    public void runPathQuery(PathQuery pathQuery, int firstResult, int maxResults,  String title,
            String description, WebServiceInput input, String mineLink, String layout) {
        Profile profile = SessionMethods.getProfile(request.getSession());
        PathQueryExecutor executor = this.im.getPathQueryExecutor(profile);
        executor.setBatchSize(BATCH_SIZE);
        ExportResultsIterator resultIt = executor.execute(pathQuery, firstResult,
                maxResults);
        ResultProcessor processor;
        if (getFormat() == WebService.JSON_OBJ_FORMAT) {   	
        	processor = new JSONObjResultProcessor();
        } else {
        	processor = new ResultProcessor();
        }
        try {
            resultIt.goFaster();
            processor.write(resultIt, output);
        } finally {
            resultIt.releaseGoFaster();
        }
        forward(pathQuery, title, description, input, mineLink, layout);
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
