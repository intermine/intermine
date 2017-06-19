package org.intermine.webservice.server;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.intermine.util.PropertiesUtil;
import org.intermine.metadata.StringUtil;
import org.intermine.web.context.InterMineContext;
import org.intermine.webservice.server.exceptions.BadRequestException;

/**
 * Base request parser that is used by advanced web service parsers.
 *
 * @author Jakub Kulaviak
 * @author Alex Kalderimis
 **/
public class WebServiceRequestParser
{
    /** The smallest legal result size you can request **/
    public static final int MIN_LIMIT = 1;

    /** Name of start parameter that determines index of first returned result. */
    public static final String START_PARAMETER = "start";

    /** Name of size parameter that determines number of returned results. */
    public static final String LIMIT_PARAMETER = "size";

    private static final Integer DEFAULT_START = new Integer(0);

    /** 10 000 000 default size actually means that web service will return all results */
    public static final Integer DEFAULT_LIMIT = new Integer(10000000);

    private static final Integer MAX_LIMIT = new Integer(10000000);

    /** Value of parameter when user wants xml output to be returned. **/
    public static final String FORMAT_PARAMETER_XML = "xml";

    /** Value of parameter when user wants tab separated output to be returned. **/
    public static final String FORMAT_PARAMETER_TAB = "tab";

    /** Value of parameter when user wants tab separated output to be returned. (alternate) **/
    public static final String FORMAT_PARAMETER_TSV = "tsv";

    /** Value of parameter when user wants plain text to be returned. **/
    public static final String FORMAT_PARAMETER_TEXT = "text";

    /** Value of parameter when user wants html output to be returned. **/
    public static final String FORMAT_PARAMETER_HTML = "html";

    /** Value of parameter when user wants comma separated output to be returned. **/
    public static final String FORMAT_PARAMETER_CSV = "csv";

    /** Value of parameter when user wants comma separated output to be returned. **/
    public static final String FORMAT_PARAMETER_COUNT = "count";

    /**
     * Value of parameter when user wants json data
    **/
    public static final String FORMAT_PARAMETER_JSON = "json";

    /**
     * Value of parameter when user wants jsonp data
    **/
    public static final String FORMAT_PARAMETER_JSONP = "jsonp";

    /**
     * Value of parameter when user wants json data in a data table
    **/
    public static final String FORMAT_PARAMETER_JSON_DATA_TABLE = "jsondatatable";

    /**
     * Value of parameter when user wants jsonp data in a data table
    **/
    public static final String FORMAT_PARAMETER_JSONP_DATA_TABLE = "jsonpdatatable";


    /**
     * Value of parameter when user wants json data as
     * nested objects representing records
    **/
    public static final String FORMAT_PARAMETER_JSON_OBJ = "jsonobjects";

    /**
     * Value of parameter when user wants json data as above,
     * but in a format suitable for cross site ajax calls
     **/
    public static final String FORMAT_PARAMETER_JSONP_OBJ = "jsonpobjects";

    /**
     * Value of parameter when user wants json data suitable
     * for using to construct tables with
     **/
    public static final String FORMAT_PARAMETER_JSON_TABLE = "jsontable";

    /**
     * Value of parameter when user wants json data as above
     * but in a format suitable for cross site ajax calls
     **/
    public static final String FORMAT_PARAMETER_JSONP_TABLE = "jsonptable";

    /**
     * Value of parameter when user wants json data suitable
     * for using to construct tables with - this returns the rows of the table
     **/
    public static final String FORMAT_PARAMETER_JSON_ROW = "jsonrows";

    /**
     * Value of parameter when user wants json data as above
     * but in a format suitable for cross site ajax calls
     **/
    public static final String FORMAT_PARAMETER_JSONP_ROW = "jsonprows";

    /**
     * Value of parameter when user wants json data suitable
     * for using to construct tables with - this returns the rows of the table
     **/
    public static final String FORMAT_PARAMETER_JSON_COUNT = "jsoncount";

    /**
     * Value of parameter when user wants json data as above
     * but in a format suitable for cross site ajax calls
     **/
    public static final String FORMAT_PARAMETER_JSONP_COUNT = "jsonpcount";

    /**Name of format parameter that specifies format of returned results. */
    public static final String OUTPUT_PARAMETER = "format";

    /** The callback to be supplied for jsonp calls **/
    public static final String CALLBACK_PARAMETER = "callback";

    /** The parameter for requesting column headers **/
    public static final String ADD_HEADER_PARAMETER = "columnheaders";

    /** The parameter for accepting any format **/
    public static final String FORMAT_PARAMETER_ANY = "*/*";

    /** The parameter for setting the filename **/
    public static final String FILENAME_PARAMETER = "filename";

    /**
     * Parses common parameters for all web services. Must be called from parseRequest
     * method in subclass else the parameters won't be set.
     * @param request request
     * @param input web service input in which the parameters are set
     */
    public void parseRequest(HttpServletRequest request, WebServiceInput input) {
        input.setLimit(DEFAULT_LIMIT);
        input.setStart(DEFAULT_START);

        Integer start = parseInteger(request.getParameter(START_PARAMETER), START_PARAMETER, 0,
                Integer.MAX_VALUE);
        if (start != null) {
            input.setStart(start);
        }

        Integer limit = parseInteger(request.getParameter(LIMIT_PARAMETER),
                LIMIT_PARAMETER, MIN_LIMIT, MAX_LIMIT.intValue());
        if (limit != null) {
            input.setLimit(limit);
        }
    }

    private Integer parseInteger(String stringValue, String name, int minValue, int maxValue) {
        Integer ret = null;
        if (stringValue != null && !"".equals(stringValue)) {
            try {
                ret = new Integer(stringValue);
                if (ret.intValue() < minValue || ret.intValue() > maxValue) {
                    throw new BadRequestException("Invalid value of " + name + " parameter: " + ret
                            + " Parameter should have value from " + minValue + " to "
                            + maxValue + ".");
                }
            } catch (Exception ex) {
                throw new BadRequestException("Invalid " + name + " parameter: " + stringValue);
            }
        }
        return ret;
    }

    private static final Map<String, Format> FORMAT_MAPPING = new HashMap<String, Format>() {
        private static final long serialVersionUID = -2791706714042933771L;
        {
            put(FORMAT_PARAMETER_ANY, Format.DEFAULT);
            put(FORMAT_PARAMETER_XML, Format.XML);
            put(FORMAT_PARAMETER_HTML, Format.HTML);
            put(FORMAT_PARAMETER_TAB, Format.TSV);
            put(FORMAT_PARAMETER_TSV, Format.TSV);
            put(FORMAT_PARAMETER_CSV, Format.CSV);
            put(FORMAT_PARAMETER_TEXT, Format.TEXT);
            put(FORMAT_PARAMETER_COUNT, Format.TEXT);
            put(FORMAT_PARAMETER_JSON_OBJ, Format.OBJECTS);
            put(FORMAT_PARAMETER_JSONP_OBJ, Format.OBJECTS);
            put(FORMAT_PARAMETER_JSON_TABLE, Format.TABLE);
            put(FORMAT_PARAMETER_JSONP_TABLE, Format.TABLE);
            put(FORMAT_PARAMETER_JSON_ROW, Format.ROWS);
            put(FORMAT_PARAMETER_JSONP_ROW, Format.ROWS);
            put(FORMAT_PARAMETER_JSONP, Format.JSON);
            put(FORMAT_PARAMETER_JSON, Format.JSON);
            put(FORMAT_PARAMETER_JSONP_COUNT, Format.JSON);
            put(FORMAT_PARAMETER_JSON_COUNT, Format.JSON);
        }
    };

    /**
     * Figure out which format the user wants.
     * @param format The format as provided by the user.
     * @return A real Format.
     */
    protected static Format interpretFormat(String format) {
        if (StringUtils.isBlank(format)) {
            return Format.EMPTY;
        }
        Format mapped = FORMAT_MAPPING.get(format);
        if (mapped == null) {
            return Format.UNKNOWN;
        }  else {
            return mapped;
        }
    }

    /**
     * Work out if this a JSON-P request.
     * @param request The incoming request.
     * @return Whether or not it is a JSON-P request.
     */
    public static boolean isJsonP(HttpServletRequest request) {
        if (!"GET".equals(request.getMethod())) {
            // All JSON-P request are via the GET method.
            return false;
        }
        String accept = request.getHeader("Accept");
        if (StringUtils.isNotBlank(accept)) {
            if (accept.startsWith("application/jsonp")) {
                return true;
            }
        }
        String formatParam = request.getParameter("format");
        if (StringUtils.isNotBlank(formatParam)) {
            if (formatParam.contains("jsonp")) {
                return true;
            }
        }
        String callback = request.getParameter("callback");
        return StringUtils.isNotBlank(callback);
    }

    private static final Map<String, String> ACCEPT_TYPES = new HashMap<String, String>() {
        private static final long serialVersionUID = -702400895288862953L;
        {
            Properties wp = InterMineContext.getWebProperties();
            Properties subset = PropertiesUtil.getPropertiesStartingWith(
                    "ws.accept.", wp);
            for (Object name : subset.keySet()) {
                String propName = String.valueOf(name);
                put(propName.substring(10), subset.getProperty(propName));
            }
        }
    };

    private static List<Format> parseAcceptHeader(HttpServletRequest request) {
        List<Format> areAcceptable = new ArrayList<Format>();

        String accept = request.getHeader("Accept");
        if (accept != null) {
            String[] preferences = accept.split(",");
            if (preferences != null) {
                for (String pref : preferences) {
                    if (pref == null) {
                        areAcceptable.add(Format.EMPTY);
                        continue;
                    }
                    pref = pref.trim().toLowerCase();
                    if (pref.startsWith("*")) {
                        areAcceptable.add(Format.DEFAULT);
                        continue;
                    }
                    String[] parts = pref.split(";");
                    String type = parts[0].trim();
                    if (ACCEPT_TYPES.containsKey(type)) {
                        areAcceptable.add(Format.valueOf(ACCEPT_TYPES.get(type)));
                    } else if ("application/json".equals(type)
                            || "text/javascript".equals(type)
                            || "application/javascript".equals(type)
                            || "application/jsonp".equals(type)) {
                        if (parts.length > 1) {
                            for (int i = 1; i < parts.length; i++) {
                                String option = parts[i].trim();
                                if (option.startsWith("type=")) {
                                    String subType = option.substring(5);
                                    if ("objects".equalsIgnoreCase(subType)) {
                                        areAcceptable.add(Format.OBJECTS);
                                        continue;
                                    } else if ("table".equalsIgnoreCase(subType)) {
                                        areAcceptable.add(Format.TABLE);
                                        continue;
                                    } else if ("rows".equalsIgnoreCase(subType)) {
                                        areAcceptable.add(Format.ROWS);
                                        continue;
                                    }
                                }
                            }
                        }
                        areAcceptable.add(Format.JSON);
                        continue;
                    } else {
                        areAcceptable.add(Format.UNKNOWN);
                    }
                }
            }
        }
        return areAcceptable;
    }

    /**
     * Parse a format from the path-info of the request. By default, if the
     * path-info is one of "xml", "json", "jsonp", "tsv" or "csv", then an
     * appropriate format will be returned. All other values will cause null to
     * be returned.
     *
     * @param request the incoming request.
     * @return A format string.
     */
    protected static Format parseFormatFromPathInfo(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        pathInfo = StringUtil.trimSlashes(pathInfo);
        if (pathInfo != null) {
            if (!pathInfo.contains("/")) {
                // This just helps with cases where the path-info
                // carries other information.
                pathInfo = "/" + pathInfo;
            }
            if (pathInfo.endsWith("/xml")) {
                return Format.XML;
            } else if (pathInfo.endsWith("/json")) {
                return Format.JSON;
            } else if (pathInfo.endsWith("/jsonp")) {
                return Format.JSON;
            } else if (pathInfo.endsWith("/tsv")) {
                return Format.TSV;
            } else if (pathInfo.endsWith("/csv")) {
                return Format.CSV;
            } else if (pathInfo.endsWith("/txt")) {
                return Format.TEXT;
            }
        }
        return null;
    }

    /**
     * Get the list of formats that this request finds acceptable.
     * @param request The incoming client request.
     * @return A list of formats this request can accept.
     */
    public static List<Format> getAcceptableFormats(HttpServletRequest request) {
        List<Format> areAcceptable = new ArrayList<Format>();

        Format fromPathInfo = parseFormatFromPathInfo(request);
        if (fromPathInfo != null) {
            areAcceptable.add(fromPathInfo);
        }
        String fromParameter = request.getParameter(OUTPUT_PARAMETER);
        if (StringUtils.isNotBlank(fromParameter)) {
            areAcceptable.add(interpretFormat(fromParameter.trim()));
        }
        areAcceptable.addAll(parseAcceptHeader(request));
        return areAcceptable;
    }

    /**
     * @param request The incoming request.
     * @return true iff this request is looking for a count.
     */
    public static boolean isCountRequest(HttpServletRequest request) {
        String doCount = request.getParameter("count");
        boolean count = Boolean.parseBoolean(doCount);
        if (!count) {
            String param = request.getParameter(WebServiceRequestParser.OUTPUT_PARAMETER);
            if (param != null && param.contains("count")) {
                count = true;
            }
        }
        if (!count) {
            String accept = request.getHeader("Accept");
            if (accept != null && accept.contains("type=count")) {
                count = true;
            }
        }
        return count;
    }
}
