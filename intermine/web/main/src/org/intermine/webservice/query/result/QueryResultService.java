package org.intermine.webservice.query.result;

/*
 * Copyright (C) 2002-2008 FlyMine
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

import org.apache.commons.collections.EnumerationUtils;
import org.intermine.objectstore.query.Results;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.results.WebResults;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.InterMineAction;
import org.intermine.webservice.PagedServiceInput;
import org.intermine.webservice.WebService;
import org.intermine.webservice.core.PathQueryExecutor;
import org.intermine.webservice.core.ResultProcessor;
import org.intermine.webservice.exceptions.InternalErrorException;
import org.intermine.webservice.output.MemoryOutput;

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
    
    private Map<Object, InterMineBag> savedBags;

    /**
     * Executes service specific logic. 
     * @param request request
     * @param response response
     */
    protected void execute(HttpServletRequest request,
            HttpServletResponse response) {

        QueryResultInput input = getInput();
        
        savedBags = new HashMap<Object, InterMineBag>();
        
        PathQueryBuilder builder = new PathQueryBuilder(input.getXml(),
        getXMLSchemaUrl(),
        request.getSession().getServletContext(), savedBags);

        PathQuery query = builder.getQuery();
        runPathQuery(query, input.getStart(), input.getMaxCount(), 
                input.isComputeTotalCount(), null, null, input, null);
    }

    private void forward(PathQuery pathQuery, String title, String description, 
            PagedServiceInput input, String mineLink) {
        List<String> columnNames = pathQuery.getViewStrings();
        if (getFormat() == WebService.HTML_FORMAT) {
            MemoryOutput mout = (MemoryOutput) output;
            request.setAttribute("columnNames", columnNames);
            request.setAttribute("rows", mout.getResults());
            request.setAttribute("title", title);
            request.setAttribute("description", description);
            request.setAttribute("currentPage", (input.getStart() - 1) / input.getMaxCount());
            request.setAttribute("baseLink", createBaseLink());
            request.setAttribute("pageSize", input.getMaxCount());
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
     * @param title title displayed in html output
     * @param description description displayed in html output
     * @param input input of web service
     * @param mineLink link pointing results of this query (template) in InterMine
     */
    public void runPathQuery(PathQuery pathQuery, int firstResult, int maxResults,  
            boolean displayTotalCount, String title, String description, 
            PagedServiceInput input, String mineLink) {
        PathQueryExecutor executor = new PathQueryExecutor(request, pathQuery);
        Results results = executor.getResults();
        
        results.setBatchSize(BATCH_SIZE);
        
        if (displayTotalCount) {
            if (getFormat() == WebService.XML_FORMAT) {
                Map<String, String> attributes = new HashMap<String, String>();
                attributes.put("totalResultsCount", "" + results.size());
                output.setHeaderAttributes(attributes);                
            }
            if (getFormat() == WebService.TSV_FORMAT) {
                List<String> list = new ArrayList<String>();
                list.add("" + results.size());
                output.addResultItem(list);
                return;
            }
        }
        
        WebResults webResults = new WebResults(pathQuery, results, pathQuery.getModel(), 
                executor.getPathToQueryNode(), 
                SessionMethods.getClassKeys(request.getSession().getServletContext()), null);
        ResultProcessor processor = new ResultProcessor(webResults, firstResult, maxResults);
        processor.write(output);              
        forward(pathQuery, title, description, input, mineLink);
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
