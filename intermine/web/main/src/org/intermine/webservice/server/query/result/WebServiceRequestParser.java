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

import javax.servlet.http.HttpServletRequest;

import org.intermine.webservice.server.WebServiceInput;
import org.intermine.webservice.server.exceptions.BadRequestException;


/**
 * Base request parser that is used by advanced web service parsers. 
 * @author Jakub Kulaviak
 **/
public class WebServiceRequestParser
{
    /**
     * Name of start parameter that determines index of first returned result.  
     */
    public static final String START_PARAMETER = "start";
    
    /**
     * Name of size parameter that determines number of returned results. 
     */
    public static final String LIMIT_PARAMETER = "size";
    
    private static final int DEFAULT_START = 0;
    
    private static final int DEFAULT_MAX_COUNT = 10;
    
    private static final int MAX_COUNT_LIMIT = 1000000;
    
    /** Value of parameter when user wants xml output to be returned. **/
    public static final String FORMAT_PARAMETER_XML = "xml";
    
    /** Value of parameter when user wants tab separated output to be returned. **/
    public static final String FORMAT_PARAMETER_TAB = "tab";
    
    /** Value of parameter when user wants html output to be returned. **/
    public static final String FORMAT_PARAMETER_HTML = "html";

    /** Value of parameter when user wants comma separated output to be returned. **/
    public static final String FORMAT_PARAMETER_CSV = "csv";
    
    /**
     * Name of format parameter that specifies format of returned results.  
     */
    public static final String OUTPUT_PARAMETER = "format";



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
                Integer.MAX_VALUE, input);
        if (start != null) {
            input.setStart(start);
        } 
    
        Integer maxCount = parseInteger(request.getParameter(LIMIT_PARAMETER), 
                LIMIT_PARAMETER, 1, MAX_COUNT_LIMIT, input);
        if (maxCount != null) {
            input.setMaxCount(maxCount);
        }        
    }
    
    private Integer parseInteger(String stringValue, String name, int minValue, int maxValue, 
            WebServiceInput input) {
        Integer ret = null;
        if (stringValue != null && !stringValue.equals("")) {
            try {
                ret = new Integer(stringValue);
                if (ret < minValue || ret > maxValue) {
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
}
