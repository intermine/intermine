package org.intermine.webservice.server;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.intermine.webservice.server.Formats.COUNT;
import static org.intermine.webservice.server.Formats.CSV;
import static org.intermine.webservice.server.Formats.HTML;
import static org.intermine.webservice.server.Formats.JSON;
import static org.intermine.webservice.server.Formats.JSONP;
import static org.intermine.webservice.server.Formats.JSONP_COUNT;
import static org.intermine.webservice.server.Formats.JSONP_OBJ;
import static org.intermine.webservice.server.Formats.JSONP_ROW;
import static org.intermine.webservice.server.Formats.JSONP_TABLE;
import static org.intermine.webservice.server.Formats.JSON_COUNT;
import static org.intermine.webservice.server.Formats.JSON_OBJ;
import static org.intermine.webservice.server.Formats.JSON_ROW;
import static org.intermine.webservice.server.Formats.JSON_TABLE;
import static org.intermine.webservice.server.Formats.TSV;
import static org.intermine.webservice.server.Formats.UNKNOWN;
import static org.intermine.webservice.server.Formats.XML;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.intermine.webservice.server.exceptions.BadRequestException;

/**
 * Base request parser that is used by advanced web service parsers.
 *
 * @author Jakub Kulaviak
 **/
public class WebServiceRequestParser
{
    /** Name of start parameter that determines index of first returned result. */
    public static final String START_PARAMETER = "start";

    /** Name of size parameter that determines number of returned results. */
    public static final String LIMIT_PARAMETER = "size";

    private static final Integer DEFAULT_START = new Integer(0);

    /** 10 000 000 default size actually means that web service will return all results */
    public static final Integer DEFAULT_MAX_COUNT = new Integer(10000000);

    private static final Integer MAX_COUNT_LIMIT = new Integer(10000000);

    /** Value of parameter when user wants xml output to be returned. **/
    public static final String FORMAT_PARAMETER_XML = "xml";

    /** Value of parameter when user wants tab separated output to be returned. **/
    public static final String FORMAT_PARAMETER_TAB = "tab";

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

    /**
     * Parses common parameters for all web services. Must be called from parseRequest
     * method in subclass else the parameters won't be set.
     * @param request request
     * @param input web service input in which the parameters are set
     */
    public void parseRequest(HttpServletRequest request, WebServiceInput input) {
        input.setMaxCount(DEFAULT_MAX_COUNT);
        input.setStart(DEFAULT_START);

        Integer start = parseInteger(request.getParameter(START_PARAMETER), START_PARAMETER, 0,
                Integer.MAX_VALUE);
        if (start != null) {
            input.setStart(start);
        }

        Integer maxCount = parseInteger(request.getParameter(LIMIT_PARAMETER),
                LIMIT_PARAMETER, 1, MAX_COUNT_LIMIT.intValue());
        if (maxCount != null) {
            input.setMaxCount(maxCount);
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

    private static final Map<String, Integer> formatCodes = new HashMap<String, Integer>() {
        private static final long serialVersionUID = -2791706714042933771L;
    {
        put(FORMAT_PARAMETER_XML, XML);
        put(FORMAT_PARAMETER_HTML, HTML);
        put(FORMAT_PARAMETER_TAB, TSV);
        put(FORMAT_PARAMETER_CSV, CSV);
        put(FORMAT_PARAMETER_COUNT, COUNT);
        put(FORMAT_PARAMETER_JSON_OBJ, JSON_OBJ);
        put(FORMAT_PARAMETER_JSONP_OBJ, JSONP_OBJ);
        put(FORMAT_PARAMETER_JSON_TABLE, JSON_TABLE);
        put(FORMAT_PARAMETER_JSONP_TABLE, JSONP_TABLE);
        put(FORMAT_PARAMETER_JSON_ROW, JSON_ROW);
        put(FORMAT_PARAMETER_JSONP_ROW, JSONP_ROW);
        put(FORMAT_PARAMETER_JSONP, JSONP);
        put(FORMAT_PARAMETER_JSON, JSON);
        put(FORMAT_PARAMETER_JSONP_COUNT, JSONP_COUNT);
        put(FORMAT_PARAMETER_JSON_COUNT, JSON_COUNT);
    }};

    protected static int interpretFormat(String format, int defaultValue) {
        if (StringUtils.isBlank(format)) {
            return defaultValue;
        }
        Integer code = formatCodes.get(format);
        if (code == null) {
            return UNKNOWN;
        } else {
            return code.intValue();
        }
    }
}
