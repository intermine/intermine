package org.intermine.webservice.server.query.result;

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

import org.intermine.webservice.server.exceptions.BadRequestException;

/**
 * Processes service request. Evaluates parameters and validates them and check if 
 * its combination is valid. 
 * 
 * @author Jakub Kulaviak
 **/
public class QueryResultRequestParser extends WebServiceRequestParser  
{
    /** Name of parameter with query **/ 
    public static final String QUERY_PARAMETER = "query";

    /** Compute total count parameter name. **/
    public static final String COMPUTE_TOTAL_COUNT_PARAMETER = "tcount";
    
    /** Layout parameter name. **/
    public static final String LAYOUT_PARAMETER = "layout";
    
    private HttpServletRequest request;
    
    /**
     * RequestProcessor constructor.
     * @param request request
     */
    public QueryResultRequestParser(HttpServletRequest request) {
        this.request = request;
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
            throw new BadRequestException("invalid " + QUERY_PARAMETER 
                    + " parameter (empty or missing)");
        } else {
            input.setXml(xmlQuery);
        }

        String totalCount = request.getParameter(COMPUTE_TOTAL_COUNT_PARAMETER);
        if (totalCount != null) {
            throw new BadRequestException("Parameter " + COMPUTE_TOTAL_COUNT_PARAMETER 
                    + " is not now supported. It is not possible to retrieve number of results.");
        }
        
        input.setLayout(request.getParameter(LAYOUT_PARAMETER));
    }
}
