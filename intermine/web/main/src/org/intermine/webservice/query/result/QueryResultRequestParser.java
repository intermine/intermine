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

import javax.servlet.http.HttpServletRequest;

/**
 * Processes service request. Evaluates parameters and validates them and check if 
 * its combination is valid. 
 * @author Jakub Kulaviak
 **/
public class QueryResultRequestParser extends WebServiceRequestParser  
{
    private static final String QUERY_PARAMETER = "query";

    /** Compute total count parameter name. **/
    public static final String COMPUTE_TOTAL_COUNT_PARAMETER = "tcount";
    
    private HttpServletRequest request;
    
    /**
     * RequestProcessor constructor.
     * @param request request
     */
    public QueryResultRequestParser(HttpServletRequest request) {
        this.request = request;
    }

    private String invalidParameterMsg(String name, String value) {
        return "invalid " + name +  " parameter: " + value;
    }

    
    /**
     * Returns parsed parameters in parameter object - so this 
     * values can be easily get from this object.
     * @return web service input
     */
    public QueryResultInput getInput() {
        QueryResultInput input = new QueryResultInput();
        parseRequest(request, input);
        return input;
    }

    private void parseRequest(HttpServletRequest request, QueryResultInput input) {

        super.parseRequest(request, input);
        
        String xmlQuery = request.getParameter(QUERY_PARAMETER);
        if (xmlQuery == null || xmlQuery.equals("")) {
            input.addError("invalid " + QUERY_PARAMETER + " parameter (empty or missing)");
        } else {
            input.setXml(xmlQuery);
        }

        String totalCount = request.getParameter(COMPUTE_TOTAL_COUNT_PARAMETER);
        if (totalCount != null) {
            input.setComputeTotalCount(true);
        }         
    }
}
