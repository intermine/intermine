package org.intermine.web.logic;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.uri.InterMineLUI;
import org.intermine.web.uri.InterMineLUIConverter;
import org.intermine.web.util.URLGenerator;

import javax.servlet.http.HttpServletRequest;
import java.util.Properties;

/**
 * Utility class used to build the permanent URI and permanent URL
 * Permanent URLs are used in the Share button and to set the attribute 'url' in Schema.org
 * Permanent URIs are used to set the attribute 'identifier' in Schema.org and in RDF
 * Examples of permanent URIs
 * 1-If the mine is registered in identifiers.org and you've set in the mine properties file
 * identifier.uri.base=identifiers.org/biotestmine, the permanent URI will be
 * identifiers.org/biotestmine/protein:P31946
 * 2-If you use a redirection system and you've set in the mine properties file
 * identifier.uri.base=purl.biotestmine.org/biotestmine, the permanent URI will be
 * purl.biotestmine.org/biotestmine/protein:P31946
 * 3-If identifier.uri.base is not set, the uri generated will be based on
 * {webapp.baseurl}/{webapp.path}/protein:P31946
 *
 * @author danielabutano
 */
public class PermanentURIHelper
{
    private HttpServletRequest request;
    private static final Logger LOGGER = Logger.getLogger(PermanentURIHelper.class);

    /**
     * Constructor
     * @param request the HttpServletRequest
     */
    public PermanentURIHelper(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * Returns the permanent base URI
     * @return the permanent base uri
     */
    public String getPermanentBaseURI() {
        final Properties webProperties = InterMineContext.getWebProperties();
        String baseURI = webProperties.getProperty("identifier.uri.base");
        if (baseURI == null || StringUtils.isEmpty(baseURI)) {
            baseURI = new URLGenerator(request).getPermanentBaseURL();
        }
        return (!baseURI.endsWith("/")) ? baseURI.concat("/") : baseURI;
    }


    /**
     * Returns the permanent URL of th entity given its internal id
     * The permanent URL is used in the Share button, to set the url in Schema.org
     * @param interMineId the internal id
     * @return the permanent url or null if can not generate the url
     */
    public String getPermanentURL(Integer interMineId) {
        InterMineLUIConverter converter = new InterMineLUIConverter();
        InterMineLUI interMineLUI = null;
        interMineLUI = converter.getInterMineLUI(interMineId);
        return getPermanentURL(interMineLUI);
    }

    /**
     * Returns the permanent URL given the lui
     * The permanent URL is used in the Share button, to set the url in Schema.org
     * @param lui the InterMine lui
     * @return the permanent url
     */
    public String getPermanentURL(InterMineLUI lui) {
        if (lui != null) {
            String baseURL = new URLGenerator(request).getPermanentBaseURL();
            if (!baseURL.endsWith("/")) {
                baseURL = baseURL + "/";
            }
            String permanentURL = baseURL + lui.toString();
            return permanentURL;
        }
        return null;
    }
}
