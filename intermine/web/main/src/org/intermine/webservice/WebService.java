package org.intermine.webservice;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.webservice.output.HTMLOutput;
import org.intermine.webservice.output.Output;
import org.intermine.webservice.output.StreamedOutput;
import org.intermine.webservice.output.TabFormatter;
import org.intermine.webservice.output.XMLFormatter;
import org.intermine.webservice.query.result.WebServiceRequestParser;

/**
 * 
 * Base class for web services. See methods of class to be able implement subclass. 
 * <h3>Output</h3> 
 * There can be 3 types of output:
 * <ul>
 * <li> Only Error output
 * <li> Complete results - xml, tab separated, html
 * <li> Incomplete results - error messages are appended at the end
 * </ul>
 * 
 * <h3>Web service design</h3>
 * <ul>
 * <li>Request is parsed with corresponding RequestProcessor class and returned as a 
 *      corresponding Input class.
 * <li>Web services are subclasses of WebService class.
 * <li>Web services use implementations of Output class to print results.
 * <li>Request parameter names are constants in corresponding RequestProcessorBase subclass.
 * <li>Servlets are used only for forwarding to corresponding web service, that is 
 *     created always new.  With this implementation fields of new service are 
 *     correctly initialized and there don't stay values from previous requests.
 * </ul>   
 * For using of web services see InterMine wiki pages.
 * @author Jakub Kulaviak
 */
public abstract class WebService
{
    /** XML format constant **/
    public static final int XML_FORMAT = 0;

    /** TSV format constant **/
    public static final int TSV_FORMAT = 1;
    
    /** HTML format constant **/
    public static final int HTML_FORMAT = 2;
    
    private static final String WEB_SERVICE_DISABLED_PROPERTY = "webservice.disabled";

    /**
     * Name of format parameter that specifies format of returned results.  
     */
    public static final String OUTPUT_PARAMETER = "format";
    
    private static Logger logger = Logger.getLogger(WebService.class);
    
    private static final String FORWARD_PATH = "/webservice/table.jsp";
    
    protected HttpServletRequest request;
    
    protected HttpServletResponse response;
    
    protected Output output;

    private void initService(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        this.request = request;
        this.response = response;
    }

    /**
     * Starting method of web service. The web service should be run like 
     * <pre>
     *   new ListsService().doGet(request, response);
     * </pre>
     * Ensures initialization of web service and makes steps common for 
     * all web services and after that executes <tt>execute</tt> method, 
     * that should be overwriten with each web service. 
     * @param request request
     * @param response response
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {

            this.request = request;
            this.response = response;
            initOutput(response);
            
            Properties webProperties = (Properties) request.getSession()
                .getServletContext().getAttribute(Constants.WEB_PROPERTIES);
            if ("true".equalsIgnoreCase(webProperties.getProperty(WEB_SERVICE_DISABLED_PROPERTY))) {
                output.addError("Web service is disabled.", Output.SC_FORBIDDEN);
                return;
            }
                 
            execute(request, response);
            if (!response.isCommitted() && output.getStatus() != Output.SC_OK) {
                response.setStatus(output.getStatus());
            }
            output.flush();
        } catch (WebServiceException ex) {
            if (ex.getMessage() != null && ex.getMessage().length() >= 0) {
                output.addError(ex.getMessage(), Output.SC_INTERNAL_SERVER_ERROR);
            } else {
                output.addError(WebServiceConstants.SERVICE_FAILED_MSG, 
                        Output.SC_INTERNAL_SERVER_ERROR);
            }
            logger.error("Service failed.", ex);
        } catch (Throwable t) {
            output.addError(WebServiceConstants.SERVICE_FAILED_MSG, 
                    Output.SC_INTERNAL_SERVER_ERROR);
            logger.error("Service failed.", t);
        }
    }

    private void initOutput(HttpServletResponse response) {
        PrintWriter out;
        try {
            out = response.getWriter();
        } catch (IOException e) {
            throw new WebServiceException("Internal error.", e);
        } 
        switch (getFormat()) {
            case XML_FORMAT: 
                output = new StreamedOutput(out, new XMLFormatter());
                ResponseUtil.setXMLContentType(response);
                break;
            case TSV_FORMAT:
                output = new StreamedOutput(out, new TabFormatter());
                ResponseUtil.setTabContentType(response);
                break;
            case HTML_FORMAT:
                output = new HTMLOutput(out);
                ResponseUtil.setHTMLContentType(response);
                break;
            default:
                throw new WebServiceException("Invalid format.");
        }        
    }
    
    /**
     * Returns required output format.
     * @return format 
     */
    public int getFormat() {
        String format = request.getParameter(OUTPUT_PARAMETER);
        if (format == null || format.equals("")) {
            return TSV_FORMAT;
        } else {
            if (WebServiceRequestParser.FORMAT_PARAMETER_XML.equalsIgnoreCase(format)) {
                return XML_FORMAT;
            } if (WebServiceRequestParser.FORMAT_PARAMETER_HTML.equalsIgnoreCase(format)) {
                return HTML_FORMAT;
            } else {
                return TSV_FORMAT;
            }
        }
    }

    /**
     * Runs service. This is abstract method, that must be defined in subclasses 
     * and so performs something useful. Standard procedure is overwrite this method 
     * in subclasses and let this method to be called from WebService.doGet method that 
     * encapsulates logic common for all web services else you can overwrite doGet 
     * method in your web service class and manage all the things alone.
     * @param request request
     * @param response response
     * @throws Exception if some error occurs
     */
    protected abstract void execute(HttpServletRequest request,
            HttpServletResponse response) throws Exception;

    /**
     * Validates input.
     * @param input input to be validated
     * @return true if it is valid or false
     */
    protected boolean validate(WebServiceInput input) {
        if (!input.isValid()) {
            output.addErrors(input.getErrors(), Output.SC_BAD_REQUEST);
            return false;
        } else {
            return true;
        }
    }
    
    /**
     * Returns dispatcher that forwards to the page that displays 
     * results as a html page.
     * @return dispatcher
     */
    public RequestDispatcher getHtmlForward() {
        return request.getSession().getServletContext().getRequestDispatcher(FORWARD_PATH);
    }
}
