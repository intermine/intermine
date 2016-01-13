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

import org.intermine.webservice.server.LinkGeneratorBase;
import org.intermine.webservice.server.WebServiceConstants;
import org.intermine.webservice.server.WebServiceRequestParser;

/**
 * Class that implements generating links for QueryResultService web service.
 *
 * @author Jakub Kulaviak
 **/
public class QueryResultLinkGenerator extends LinkGeneratorBase
{

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
        return baseUrl + getLinkPath(queryXml, resultFormat);
    }

    /**
     * Get the link path for a query.
     * @param queryXml The query represented as XML.
     * @param resultFormat The desired result format.
     * @return A string representing the path section of a webservice URL.
     */
    public String getLinkPath(String queryXml, String resultFormat) {
        return WebServiceConstants.MODULE_NAME + "/query/results"
            + "?" + QueryResultRequestParser.QUERY_PARAMETER + "=" + encode(queryXml) + "&"
            + WebServiceRequestParser.OUTPUT_PARAMETER + "=" + resultFormat;
    }

    /**
     * Get the link for a query that shows the results in a mine.
     * @param baseUrl The base URL of the mine.
     * @param queryXml The query represented as XML.
     * @return A string representing a web-app URL.
     */
    public String getMineResultsLink(String baseUrl, String queryXml) {
        return baseUrl + getMineResultsPath(queryXml);
    }

    /**
     * Get the link path for a query that shows the results in a mine.
     * @param queryXml The query represented as XML.
     * @return A string representing the path section of a web-app URL.
     */
    public String getMineResultsPath(String queryXml) {
        String ret = "/loadQuery.do?";
        ret += "skipBuilder=true";
        ret += "&" + QueryResultRequestParser.QUERY_PARAMETER + "=" + encode(queryXml);
        ret += "&method=xml";
        return ret;
    }

}
