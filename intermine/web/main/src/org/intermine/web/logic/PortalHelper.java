package org.intermine.web.logic;

/*
 * Copyright (C) 2002-2011 FlyMine
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
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.FastPathObject;
import org.intermine.util.DynamicUtil;
import org.intermine.web.logic.bag.BagConverter;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.util.URLGenerator;


/**
 * Util methods for the portal
 * @author Julie Sullivan
 **/
public final class PortalHelper
{
    private static Map<String, BagConverter> bagConverters = new HashMap<String, BagConverter>();
    private static String portalBaseUrl = null;

    private static final String INTERNAL_REPORT_PAGE = "objectDetails.do";
    private static final String EXTERNAL_PORTAL_PAGE = "portal.do";

    private PortalHelper() {
    }

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

    /**
     * Generate a stable link to a report page for the given object, this will create a portal link
     * with the correct class and a value from a non-null class key field of the object.  Will
     * return null if there is no non-null value or class key available.
     * @param obj the object to link to
     * @param im the InterMineApi
     * @param request the request object
     * @return a portal URL to the object or null
     */
    public static String generatePortalLink(FastPathObject obj, InterMineAPI im,
            HttpServletRequest request) {
        Map<String, List<FieldDescriptor>> classKeys = im.getClassKeys();
        String baseUrl = getBaseUrl(request);
        return generatePermaLink(obj, baseUrl, classKeys);
    }

    public static String generatePortalPath(FastPathObject obj, InterMineAPI im) {
        Map<String, List<FieldDescriptor>> classKeys = im.getClassKeys();
        return generatePermaPath(obj, classKeys);
    }

    /**
     * Generate a perma-link to a report page for an InterMine object.
     * @param obj A Fast-Path Object
     * @param baseUrl The base url to use
     * @param classKeys The Class keys for this web-app
     * @return The url
     */
    public static String generatePermaLink(FastPathObject obj, String baseUrl,
            Map<String, List<FieldDescriptor>> classKeys) {
        return  baseUrl + generatePermaPath(obj, classKeys);
    }

    public static String generatePermaPath(FastPathObject obj, Map<String, List<FieldDescriptor>> classKeys) {
        String url = null;
        Object externalId = ClassKeyHelper.getKeyFieldValue(obj, classKeys);
        if (externalId != null) {
            String clsName = DynamicUtil.getSimpleClass(obj).getSimpleName();
            StringBuilder sb = new StringBuilder();
            sb.append("/").append(EXTERNAL_PORTAL_PAGE).append("?class=");
            sb.append(clsName);
            sb.append("&externalids=");
            sb.append(encode(externalId.toString()));
            url = sb.toString();
        }
        return url;
    }


    /**
     * Generate a link to the object details page using the internal id. This does not produce
     * a link suitable for use as a permalink.
     * @param elem a result element
     * @param baseUrl The base URL to use to create the link.
     * @return The URL.
     */
    public static String generateObjectDetailsLink(ResultElement elem, String baseUrl) {
        return baseUrl + generateObjectDetailsPath(elem);
    }

    public static String generateObjectDetailsPath(ResultElement elem) {
        String url = null;
        StringBuilder sb = new StringBuilder();
        sb.append("/").append(INTERNAL_REPORT_PAGE).append("?id=");
        sb.append(elem.getId().toString());
        url = sb.toString();
        return url;
    }

    public static String getBaseUrl(HttpServletRequest request) {
        if (portalBaseUrl == null) {
            portalBaseUrl = new URLGenerator(request).getPermanentBaseURL();
        }
        return portalBaseUrl;
    }

    private static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }
}
