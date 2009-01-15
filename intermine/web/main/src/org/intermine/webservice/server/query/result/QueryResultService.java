package org.intermine.webservice.server.query.result;

/*
 * Copyright (C) 2002-2009 FlyMine
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections.EnumerationUtils;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.query.PathQueryExecutor;
import org.intermine.web.logic.results.ResultElement;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.InterMineAction;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.WebServiceInput;
import org.intermine.webservice.server.core.ResultProcessor;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.intermine.webservice.server.output.MemoryOutput;

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
    
    private Map<String, InterMineBag> savedBags;

    /**
     * Executes service specific logic. 
     * @param request request
     * @param response response
     */
    protected void execute(HttpServletRequest request,
            HttpServletResponse response) {

        QueryResultInput input = getInput();
        
        HttpSession session = request.getSession();
        savedBags = WebUtil.getAllBags(SessionMethods.getProfile(session).getSavedBags(), 
                SessionMethods.getSearchRepository(session.getServletContext()));

        PathQueryBuilder builder = new PathQueryBuilder(input.getXml(), getXMLSchemaUrl(),
                request.getSession().getServletContext(), savedBags);

        PathQuery query = builder.getQuery();
        runPathQuery(query, input.getStart(), input.getMaxCount(), 
                input.isComputeTotalCount(), null, null, input, null, input.getLayout());
    }

    private void forward(PathQuery pathQuery, String title, String description, 
            WebServiceInput input, String mineLink, String layout) {
        List<String> columnNames = pathQuery.getViewStrings();
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
     * @param displayTotalCount if total result count should be displayed
     * @param title title displayed in html output, can be null
     * @param description description displayed in html output, can be null
     * @param input input of web service
     * @param mineLink link pointing results of this query (template) in InterMine, can be null
     * @param layout results table layout string, can be null
     */
    public void runPathQuery(PathQuery pathQuery, int firstResult, int maxResults,  
            boolean displayTotalCount, String title, String description, 
            WebServiceInput input, String mineLink, String layout) {
        PathQueryExecutor executor = SessionMethods.getPathQueryExecutor(request.getSession());
        executor.setBatchSize(BATCH_SIZE);
        Iterator<List<ResultElement>> resultIt = executor.execute(pathQuery, firstResult, 
                maxResults);

        // displayTotalCount now without effect because information about results size
        // is not available because of the implementation of the object store outer join 
        new ResultProcessor().write(resultIt, output);              
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
