package org.intermine.webservice.server.query.result;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.query.QueryStore;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.query.QueryRequestParser;

/**
 * Processes service request. Evaluates parameters and validates them and check if
 * its combination is valid.
 *
 * @author Jakub Kulaviak
 **/
public class QueryResultRequestParser extends QueryRequestParser
{
    /** Name of parameter with query **/
    public static final String QUERY_PARAMETER = "query";

    /** Compute total count parameter name. **/
    public static final String COMPUTE_TOTAL_COUNT_PARAMETER = "tcount";

    /** Layout parameter name. **/
    public static final String LAYOUT_PARAMETER = "layout";

    /**
     * RequestProcessor constructor.
     * @param request request
     * @param queryStore A place to lookup qids in.
     */
    public QueryResultRequestParser(QueryStore queryStore, HttpServletRequest request) {
        super(queryStore, request);
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

    private void parseRequest(HttpServletRequest req, QueryResultInput input) {

        super.parseRequest(req, input);
        String xmlQuery = getQueryXml();
        if (StringUtils.isEmpty(xmlQuery)) {
            throw new BadRequestException("invalid " + QUERY_PARAMETER
                    + " parameter (empty or missing)");
        }
        input.setXml(xmlQuery);

    }
}
