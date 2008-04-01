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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.Results;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.results.WebResults;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.WebService;
import org.intermine.webservice.WebServiceConstants;
import org.intermine.webservice.WebServiceException;
import org.intermine.webservice.core.PathQueryExecutor;
import org.intermine.webservice.core.ResultProcessor;
import org.intermine.webservice.output.HTMLTable;
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

    private static final Logger LOG = Logger.getLogger(SessionMethods.class);
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
        if (!validate(input)) { return; }
        
        savedBags = new HashMap<Object, InterMineBag>();
        
        PathQueryBuilder builder = new PathQueryBuilder(input.getXml(),
                getXMLSchemaUrl(),
                request.getSession().getServletContext(), savedBags);
        if (builder.isQueryValid()) {
            try {
                PathQuery query = builder.getQuery();
                runPathQuery(query, input.getStart() - 1 , input.getMaxCount(), true,
                        null, null);
            } catch (Throwable t) {
                LOG.error("Execution of web service request failed.", t);
                output.addError("Execution of web service failed. Please contact support.");
            }
        } else {
            output.addErrors(builder.getErrors());
        }
    }

    private void forward(PathQuery pathQuery, String title, String description) {
        List<String> columnNames = pathQuery.getViewStrings();
        if (getFormat() == WebService.HTML_FORMAT) {
            MemoryOutput mout = (MemoryOutput) output;
            HTMLTable table = new HTMLTable();
            table.setColumnNames(columnNames);
            table.setRows(mout.getResults());
            table.setTitle(title);
            table.setDescription(description);
            request.setAttribute(WebServiceConstants.HTML_TABLE_ATTRIBUTE, table);
            try {
                getHtmlForward().forward(request, response);
            } catch (Exception e) {
                throw new WebServiceException(WebServiceConstants.SERVICE_FAILED_MSG, e);
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
     */
    public void runPathQuery(PathQuery pathQuery, int firstResult, int maxResults,  
            boolean displayTotalCount, String title, String description) {
        PathQueryExecutor executor = new PathQueryExecutor(request, pathQuery);
        Results results = executor.getResults();
        
        results.setBatchSize(BATCH_SIZE);
        
        if (displayTotalCount) {
            Map<String, String> attributes = new HashMap<String, String>();
            attributes.put("totalResultsCount", "" + results.size());
            output.setHeaderAttributes(attributes);
        }
        
        WebResults webResults = new WebResults(pathQuery, results, pathQuery.getModel(), 
                executor.getPathToQueryNode(), 
                SessionMethods.getClassKeys(request.getSession().getServletContext()), null);
        ResultProcessor processor = new ResultProcessor(webResults, firstResult, maxResults);
        processor.write(output);              
        forward(pathQuery, title, description);
    }

    private String getXMLSchemaUrl() {
        try {
            String relPath = request.getContextPath() + "/"
                    + XML_SCHEMA_LOCATION;
            URL url = new URL(request.getScheme(), request.getServerName(),
                    request.getServerPort(), relPath);
            return url.toString();
        } catch (MalformedURLException e) {
            throw new WebServiceException("Invalid resource location.", e);
        }
    }
   
    private QueryResultInput getInput() {
        return new QueryResultRequestProcessor(request).getInput();
    }
    
    private ObjectStore getObjectStore() {
        return (ObjectStore) request.getSession().getServletContext().
            getAttribute(Constants.OBJECTSTORE);
    }
}
