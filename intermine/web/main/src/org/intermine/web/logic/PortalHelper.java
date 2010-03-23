package org.intermine.web.logic;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.web.logic.bag.BagConverter;
import org.intermine.web.logic.config.WebConfig;


/**
 * Util methods for the portal
 * @author Julie Sullivan
 **/
public class PortalHelper
{
    private static Map<String, BagConverter> bagConverters = new HashMap();

    /**
     * If the given param is present in the comma-separated list in the first element of the given
     * paramArray, then return the param, otherwise return null.
     *
     * @param param a String
     * @param paramArray an array of Strings, the first element of which is a comma-separated list
     * @return param if it is present in the list
     */
    public static String getAdditionalParameter(String param, String[] paramArray) {
        String[] urlFields = paramArray[0].split(",");
        for (String urlField : urlFields) {
            // if one of the request vars matches the variables listed in the bagquery
            // config, add the variable to be passed to the custom converter

            if (urlField.equals(param)) {
                // the spaces in organisms, eg. D.%20rerio, need to be handled
                return param;
            }
        }
        return null;
    }

    /**
     * Searches a request for additional parameters that might match an additional converter. The
     * additional converter config is passed in through the paramArray parameter, which is a String
     * array, where the first element is a comma-separated list of possible parameter names to
     * search for. The value of the last parameter in the request that matches one of the names in
     * the list will be returned, or null if none match.
     *
     * @param request a request to search in
     * @param paramArray an array of Strings, the first element of which is a comma-separated list
     * of parameter names to search for in the request
     * @return a parameter value from the request, or null if none is found
     */
    public static String getAdditionalParameter(HttpServletRequest request, String[] paramArray) {
        String[] urlFields = paramArray[0].split(",");
        String addparameter = null;
        for (String urlField : urlFields) {
            // if one of the request vars matches the variables listed in the bagquery
            // config, add the variable to be passed to the custom converter
            String param = request.getParameter(urlField);
            if (StringUtils.isNotEmpty(param)) {
                // the spaces in organisms, eg. D.%20rerio, need to be handled
                try {
                    addparameter = URLDecoder.decode(param, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    // UTF8 not supported!
                    throw new Error("UTF-8 is not supported?!", e);
                }
            }
        }
        return addparameter;
    }

    /**
     * Returns a BagConverter for the given parameters.
     *
     * @param im the InterMine API to use
     * @param webConfig the WebConfig to take configuration from
     * @param converterClassName the class name of the converter
     * @return a new or recycled BagConverter object
     */
    public static synchronized BagConverter getBagConverter(InterMineAPI im, WebConfig webConfig,
            String converterClassName) {
        BagConverter bagConverter = bagConverters.get(converterClassName);

        if (bagConverter == null) {
            try {
                Class clazz = Class.forName(converterClassName);
                Constructor constructor = clazz.getConstructor(InterMineAPI.class, WebConfig.class);
                bagConverter = (BagConverter) constructor.newInstance(im, webConfig);
            } catch (Exception e) {
                throw new RuntimeException("Failed to construct bagconverter for "
                        + converterClassName, e);
            }
            bagConverters.put(converterClassName, bagConverter);
        }
        return bagConverter;
    }
}
