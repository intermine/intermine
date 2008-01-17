package org.intermine.webservice;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.path.Path;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.query.MainHelper;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.results.WebResults;
import org.intermine.web.logic.session.SessionMethods;

/**
 * For using of web service see InterMine wiki pages.
 * Executes web service. From xml parses parameters and xml query. 
 * 1) Validates parameters and tries validate xml query as much as possible. Validates xml 
 * query according to XML Schema and and finds out if there were some errors during unmarshalling 
 * PathQuery from xml.  
 * 2) Executes created PathQuery.
 * 3) Print results to output.
 *
 * Output: There can be 3 types of output:
 * a) Only Error output
 * b) Complete results - xml formatted or tab separated
 * c) Incomplete results - at the end are error messages 
 * @author Jakub Kulaviak
 **/
public class ServiceExecutor 
{

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(SessionMethods.class);
    private static final String XML_SCHEMA_LOCATION = "webservice/v1/query.xsd";
    private static final int BATCH_SIZE = 5000;

    private HttpServletRequest request;
    private Map<String, String> outputInfo;
    private Formatter formatter;
    private Map<Object, InterMineBag> savedBags;

    /**
     * Runs service.  
     * @param request request
     * @param response response
     * @throws ServletException ServletException
     * @throws IOException IOException
     */
    public void runService(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        PrintWriter out = response.getWriter();
        this.request = request; 
        outputInfo = new HashMap<String, String>();
        savedBags = new HashMap<Object, InterMineBag>();

        RequestProcessor processor = new RequestProcessor(request);
        if (processor.isRequestValid()) {
            WebServiceInput input = processor.getWebServiceInput();
            initFormatter(input.isXmlFormat(), out);
            PathQueryBuilder builder = new PathQueryBuilder(input.getXml(),
                    getXMLSchemaUrl(),
                    request.getSession().getServletContext(), savedBags);
            if (builder.isQueryValid()) {
                try {
                    runPathQuery(builder.getQuery(), input);
                } catch (Throwable t) {
                    LOG.error("Execution of web service request failed.", t);
                    formatter
                            .printError("Execution of web service failed. Please contact support.");
                }
            } else {
                formatter.printErrors(builder.getErrors());
            }
        } else {
            initFormatter(processor.getWebServiceInput().isXmlFormat(), out);
            formatter.printErrors(processor.getErrors());
        }
        formatter.printFooter();
    }

    /**
     * Prepares object store query from path query and runs it.
     * @param pathQuery
     * @param input
     */
    private void runPathQuery(PathQuery pathQuery, WebServiceInput input) {
        ServletContext servletContext = request.getSession()
                .getServletContext();
        ObjectStore os = (ObjectStore) servletContext
                .getAttribute(Constants.OBJECTSTORE);
        Map<String, QueryNode> pathToQueryNode = new HashMap<String, QueryNode>();
        Query query;
        try {
            // makeQuery initializes pathToQueryNode
            query = MainHelper.makeQuery(pathQuery, savedBags, pathToQueryNode,
                    servletContext, null);
        } catch (ObjectStoreException ex) {
            throw new WebServiceException("Making InterMineQuery failed.", ex);
        }
        try {
            runQuery(input, pathToQueryNode, pathQuery, query, os);
        } catch (ObjectStoreException ex) {
            throw new WebServiceException("Execution of query failed.", ex);
        }
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

    private void runQuery(WebServiceInput input,
            Map<String, QueryNode> pathToQueryNode, PathQuery pathQuery,
            Query query, ObjectStore os) throws ObjectStoreException {
        // Because web service start is 1-based
        int start = input.getStart() - 1;
        outputInfo.put("firstResultPosition", "" + input.getStart());
        Map<Object, Integer> sequence = os.getSequence(os
                .getComponentsForQuery(query));
        if (input.isComputeTotalCount()) {
            int totalCount = os.count(query, sequence);
            outputInfo.put("totalResultsAvailable", "" + totalCount);
            outputInfo.put("totalResultsReturned", ""
                    + computeResultCount(totalCount, input));
        }
        if (input.isOnlyTotalCount()) {
            formatter.getOut().print(os.count(query, sequence));
            return;
        }

        Results resultsObject = os.execute(query);
        resultsObject.setBatchSize(BATCH_SIZE);
        formatter.printHeader(outputInfo);
        int end = input.getMaxCount() + start;
        for (int i = start; i < end; i++) {
            try {
                ResultsRow resultsRow = (ResultsRow) resultsObject.get(i);
                List<String> stringRow = parseResultRow(resultsRow, pathQuery,
                        os.getModel(), WebResults.getPathToIndex(query,
                                pathToQueryNode));
                formatter.printResultItem(stringRow);
            } catch (IndexOutOfBoundsException e) {
                break;
            }
        }
    }

    private int computeResultCount(int totalCount, WebServiceInput input) {
        int count1 = totalCount - (input.getStart() - 1);
        if (count1 < 0) {
            count1 = 0;
        }
        int count2 = input.getMaxCount();
        return count1 < count2 ? count1 : count2;
    }

    private void initFormatter(boolean isXmlFormat, PrintWriter out) {
        if (isXmlFormat) {
            formatter = new XMLFormatter(out);
        } else {
            formatter = new TSVFormatter(out);
        }
    }

//    private void printInfo(PrintWriter out) {
//        // print all parameters
//        Enumeration<String> names = request.getParameterNames();
//        while (names.hasMoreElements()) {
//            String name = names.nextElement();
//            out.print(name + " : " + request.getParameter(name));
//        }
//    }

    private List<String> parseResultRow(ResultsRow resultsRow,
            PathQuery pathQuery, Model model, LinkedHashMap pathToIndex) {
        ArrayList<String> ret = new ArrayList<String>();
        for (Iterator<Path> iter = pathQuery.getView().iterator(); iter
                .hasNext();) {
            Path columnPath = (Path) iter.next();
            String columnName = columnPath.toStringNoConstraints();
            int columnIndex = ((Integer) pathToIndex.get(columnName))
                    .intValue();
            Object o = resultsRow.get(columnIndex);
            String type = TypeUtil.unqualifiedName(columnPath
                    .getLastClassDescriptor().getName());
            String fieldName = columnName
                    .substring(columnName.lastIndexOf(".") + 1);
            Path path = new Path(model, type + '.' + fieldName);
            Object fieldValue = path.resolve(o);
            if (fieldValue != null) {
                ret.add(fieldValue.toString());
            } else {
                ret.add("");
            }
        }
        return ret;
    }
}