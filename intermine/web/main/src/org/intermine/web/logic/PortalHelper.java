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
//    private static final Logger LOG = Logger.getLogger(PortalHelper.class);
    private static Map<String, BagConverter> bagConverters = new HashMap<String, BagConverter>();
    private static String portalBaseUrl = null;

    private static final String INTERNAL_REPORT_PAGE = "report.do";
    private static final String EXTERNAL_PORTAL_PAGE = "portal.do";

    private PortalHelper() {
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
                Class<?> clazz = Class.forName(converterClassName);
                Constructor<?> constructor
                    = clazz.getConstructor(InterMineAPI.class, WebConfig.class);
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
     * Searches a request for additional parameters that might match an additional converter. The
     * additional converter config is passed in through the paramArray parameter, which is a String
     * array, where the first element is a comma-separated list of possible parameter names to
     * search for. The value of the last parameter in the request that matches one of the names in
     * the list will be returned, or null if none match.
     *
     * @param request a request to search in
     * @param params comma-separated list of parameter names to search for in the request
     * @return a parameter value from the request, or null if none is found
     */
    public static String getAdditionalParameter(HttpServletRequest request, String params) {
        if (StringUtils.isEmpty(params)) {
            return null;
        }
        String[] urlFields = params.split("[, ]+");
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
     * Generate a stable link to a report page for the given object, this will create a portal link
     * with the correct class and a value from a non-null class key field of the object.  Will
     * return null if there is no non-null value or class key available.
     * @param obj the object to link to
     * @param im the InterMineApi
     * @param request the request object
     * @return a portal URL to the object or null
     * @see generatePermaLink
     */
    public static String generatePortalLink(FastPathObject obj, InterMineAPI im,
            HttpServletRequest request) {
        Map<String, List<FieldDescriptor>> classKeys = im.getClassKeys();
        String baseUrl = getBaseUrl(request);
        return generatePermaLink(obj, baseUrl, classKeys);
    }

    /**
     * Generate an external portal link (perma-link) for an InterMine object.
     * @param obj The object to link to.
     * @param im The InterMine API configuration bundle.
     * @return A path, beginning with "/" suitable for appending to a base URL.
     * @see generatePermaPath
     */
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
        String newBase = null;

        if (baseUrl.contains("release")) {
            newBase = baseUrl.replaceFirst("release-\\d*.\\d*", "query");
        } else {
            newBase = baseUrl;
        }
        return  newBase + generatePermaPath(obj, classKeys);
    }

    /**
     * Generate a link suitable for use as an external, permanent link, in that the link should work
     * between rebuilds of the database.
     * @param obj The object to link to.
     * @param classKeys The class-key configuration for determining which fields to
     * use for identification.
     * @return A path, beginning with "/" suitable for appending to a base url.
     */
    public static String generatePermaPath(FastPathObject obj, Map<String,
            List<FieldDescriptor>> classKeys) {
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
    public static String generateReportLink(ResultElement elem, String baseUrl) {
        return baseUrl + generateReportPath(elem);
    }

    /**
     * Get the path fragment (starting with "/") for the report page for an object in the mine.
     * @param elem The element containing data related to this object.
     * @return A path fragment suitable for appending to a base URL.
     * The generated path is not suitable for permanent
     * links, as it will include the internal id, which is liable to change between releases.
     */
    public static String generateReportPath(ResultElement elem) {
        String url = null;
        StringBuilder sb = new StringBuilder();
        sb.append("/").append(INTERNAL_REPORT_PAGE).append("?id=");
        sb.append(elem.getId().toString());
        url = sb.toString();
        return url;
    }


    /**
     * Get the base url for this web-app. This includes the host and context path fragment.
     * @param request An incoming request.
     * @return The base URL.
     */
    public static String getBaseUrl(HttpServletRequest request) {
        if (portalBaseUrl == null) {
            portalBaseUrl = new URLGenerator(request).getPermanentBaseURL();
        }
        return portalBaseUrl;
    }

    /**
     * URL encode a string. This method wraps java.net.URLEncoder's method,
     * returning the input string in the case of failure.
     * @param s The string to encode.
     * @return A legally encoded UTF-8 string, conforming to RFC3986.
     */
    private static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }
}
