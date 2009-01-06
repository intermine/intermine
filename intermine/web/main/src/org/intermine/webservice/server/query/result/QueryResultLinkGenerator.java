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

import org.intermine.webservice.server.LinkGeneratorBase;
import org.intermine.webservice.server.WebServiceConstants;

/**
 * Class that implements generating links for QueryResultService web service.
 * @author Jakub Kulaviak
 **/
public class QueryResultLinkGenerator extends LinkGeneratorBase 
{

    /**
     * Default value of size parameter 
     */
    private static final int DEFAULT_RESULT_SIZE = 10;

    /**
     * Generates QueryResultService web service link.
     * @param baseUrl base url e.g. http://www.flymine.org/release-12.0
     * @param queryXml query xml
     * @param resultFormat result format
     * @return generated link
     */
    public String getLink(String baseUrl, String queryXml, String resultFormat) {
        
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        } 
        return baseUrl + WebServiceConstants.MODULE_NAME + "/query/results" 
            + "?" + WebServiceRequestParser.LIMIT_PARAMETER + "=" + DEFAULT_RESULT_SIZE 
            + "&" + QueryResultRequestParser.QUERY_PARAMETER + "=" + encode(queryXml) + "&" 
            + WebServiceRequestParser.OUTPUT_PARAMETER + "=" + resultFormat;
    }
    
}
