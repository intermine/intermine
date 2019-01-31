package org.intermine.web.logic;

/*
 * Copyright (C) 2002-2018 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.uri.InterMineLUI;
import org.intermine.api.uri.InterMineLUIConverter;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.util.URLGenerator;

import javax.servlet.http.HttpServletRequest;
import java.util.Properties;

/**
 * Utility class used to build the permanent URI
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
     * Returns the permanent URI given the intermine lui, e.g. protein:P31946
     * @param lui intermine lui
     * @return the permanent uri
     */
    public String getPermanentURI(InterMineLUI lui) {
        String permanentURI = null;
        String baseURI = getPermanentBaseURI();
        if (!baseURI.endsWith("/")) {
            baseURI = baseURI + "/";
        }
        permanentURI = baseURI + lui.toString();
        return permanentURI;
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
        return baseURI;
    }


    /**
     * Returns the permanent URL given the class name and the primary identifier
     * The permanent URL is used in the Share button, to set the url in Schema.org
     * @param type the class name
     * @param interMineId the internal id or null if can not generate the url
     * @return the permanent url
     */
    public String getPermanentURL(String type, Integer interMineId) {
        InterMineLUIConverter converter = new InterMineLUIConverter();
        InterMineLUI interMineLUI = null;
        try {
            interMineLUI = converter.getInterMineLUI(type, interMineId);
        } catch (ObjectStoreException ex) {
            LOGGER.error("Problems retrieving identifier from InterMineObjectStore");
        }
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
