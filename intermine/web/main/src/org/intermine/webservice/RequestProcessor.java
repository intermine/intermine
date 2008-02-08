package org.intermine.webservice;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

/**
 * Processes service request. Evaluates parameters and validates them and check if
 * its combination is valid.
 * @author Jakub Kulaviak
 **/
public class RequestProcessor
{

    private List<String> errors = new ArrayList<String>();
    private WebServiceInput input;
    private static final String QUERY_PARAMETER = "query";
    private static final String START_PARAMETER = "start";
    private static final String MAX_COUNT_PARAMETER = "maxCount";
    private static final String COMPUTE_TOTAL_COUNT_PARAMETER = "totalCount";
    private static final String RETURN_ONLY_TOTAL_COUNT_PARAMETER = "onlyTotalCount";
    private static final String FORMAT_PARAMETER = "format";

    private static final int DEFAULT_START = 1;
    private static final int DEFAULT_MAX_COUNT = 1000;
    private static final int MAX_COUNT_LIMIT = 100000;

    /**
     * RequestProcessor constructor.
     * @param request request
     */
    public RequestProcessor(HttpServletRequest request) {
        input = new WebServiceInput();
        input.setMaxCount(DEFAULT_MAX_COUNT);
        input.setStart(DEFAULT_START);

        String xmlQuery = request.getParameter(QUERY_PARAMETER);
        if (xmlQuery == null || xmlQuery.equals("")) {
            addError("invalid " + QUERY_PARAMETER + " parameter (empty or missing)");
        } else {
            input.setXml(xmlQuery);
        }

        Integer start = parseInteger(request.getParameter(START_PARAMETER), START_PARAMETER, 1,
                Integer.MAX_VALUE);
        if (start != null) {
            input.setStart(start);
        }

        Integer maxCount = parseInteger(request.getParameter(MAX_COUNT_PARAMETER),
                MAX_COUNT_PARAMETER, 1, MAX_COUNT_LIMIT);
        if (maxCount != null) {
            input.setMaxCount(maxCount);
        }

        String totalCount = request.getParameter(COMPUTE_TOTAL_COUNT_PARAMETER);
        if (totalCount != null && !totalCount.equals("")) {
            if ("yes".equalsIgnoreCase(totalCount)) {
                input.setComputeTotalCount(true);
            } else {
                if (!"no".equalsIgnoreCase(totalCount)) {
                    addError(invalidParameterMsg(COMPUTE_TOTAL_COUNT_PARAMETER, totalCount));
                }
            }
        }

        String format = request.getParameter(FORMAT_PARAMETER);
        if (format == null || format.equals("")) {
            input.setFormat(WebServiceInput.TSV_FORMAT);
        } else {
            if (WebServiceInput.XML_FORMAT.equalsIgnoreCase(format)) {
                input.setFormat(WebServiceInput.XML_FORMAT);
            } else {
                if ((WebServiceInput.TSV_FORMAT.equalsIgnoreCase(format))) {
                    input.setFormat(WebServiceInput.TSV_FORMAT);
                } else {
                    addError(invalidParameterMsg(FORMAT_PARAMETER, format));
                }
            }
        }

        String onlyTotalCount = request.getParameter(RETURN_ONLY_TOTAL_COUNT_PARAMETER);
        if (onlyTotalCount != null && !onlyTotalCount.equalsIgnoreCase("")) {
            if ("yes".equalsIgnoreCase(onlyTotalCount)) {
                input.setOnlyTotalCount(true);
                // when only total count is requested, than only tsv format is permitted
                input.setFormat(WebServiceInput.TSV_FORMAT);
                if (input.isXmlFormat()) {
                    errors.add("only " + WebServiceInput.TSV_FORMAT + " " + FORMAT_PARAMETER
                            + " is permitted when returning only total count.");
                }
            } else {
                if (!"no".equalsIgnoreCase(onlyTotalCount)) {
                    errors.add(invalidParameterMsg(RETURN_ONLY_TOTAL_COUNT_PARAMETER,
                            onlyTotalCount));
                }
            }
        }
    }

    private String invalidParameterMsg(String name, String value) {
        return "invalid " + name +  " parameter: " + value;
    }

    private Integer parseInteger(String stringValue, String name, int minValue, int maxValue) {
        Integer ret = null;
        if (stringValue != null && !stringValue.equals("")) {
            try {
                ret = new Integer(stringValue);
                if (ret < minValue || ret > maxValue) {
                    addError("Invalid value of " + name + " parameter: " + ret
                            + " Parameter should have value from " + minValue + " to "
                            + maxValue + ".");
                }
            } catch (Exception ex) {
                String value = stringValue;
                addError("invalid " + name + " parameter: " + value);
            }
        }
        return ret;
    }

    private void addError(String error) {
        errors.add(error);
    }

    /**
     * Returns parsed parameters in parameter object - so this
     * values can be easily get from this object.
     * @return web service input
     */
    public WebServiceInput getWebServiceInput() {
        return input;
    }

    /**
     * Returns errors that could happen during parameter validation.
     * @return errors
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * Returns true if the  request has valid parameters and its
     * combination is valid.
     * @return true if request is valid
     */
    public boolean isRequestValid() {
        return errors.size() == 0;
    }
}
