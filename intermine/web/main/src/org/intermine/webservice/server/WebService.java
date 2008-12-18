package org.intermine.webservice.server;

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
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;
import org.intermine.webservice.server.output.CSVFormatter;
import org.intermine.webservice.server.output.HTMLOutput;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.StreamedOutput;
import org.intermine.webservice.server.output.TabFormatter;
import org.intermine.webservice.server.output.XMLFormatter;
import org.intermine.webservice.server.query.result.WebServiceRequestParser;

/**
 * 
 * Base class for web services. See methods of class to be able implement
 * subclass. <h3>Output</h3> There can be 3 types of output:
 * <ul>
 * <li>Only Error output
 * <li>Complete results - xml, tab separated, html
 * <li>Incomplete results - error messages are appended at the end
 * </ul>
 * 
 * <h3>Web service design</h3>
 * <ul>
 * <li>Request is parsed with corresponding RequestProcessor class and returned
 * as a corresponding Input class.
 * <li>Web services are subclasses of WebService class.
 * <li>Web services use implementations of Output class to print results.
 * <li>Request parameter names are constants in corresponding
 * RequestProcessorBase subclass.
 * <li>Servlets are used only for forwarding to corresponding web service, that
 * is created always new. With this implementation fields of new service are
 * correctly initialized and there don't stay values from previous requests.
 * </ul>
 * For using of web services see InterMine wiki pages.
 * 
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

    /** CSV format constant **/
    public static final int CSV_FORMAT = 3;
    
    private static final String WEB_SERVICE_DISABLED_PROPERTY = "webservice.disabled";

    private static Logger logger = Logger.getLogger(WebService.class);

    private static final String FORWARD_PATH = "/webservice/table.jsp";

    protected HttpServletRequest request;

    protected HttpServletResponse response;

    protected Output output;

    /**
     * Starting method of web service. The web service should be run like
     * 
     * <pre>
     * new ListsService().doGet(request, response);
     * </pre>
     * 
     * Ensures initialization of web service and makes steps common for all web
     * services and after that executes <tt>execute</tt> method, that should be
     * overwritten with each web service.
     * 
     * @param request request
     * @param response response
     */
    public void service(HttpServletRequest request, HttpServletResponse response) {
        try {

            this.request = request;
            this.response = response;
            initOutput(response);

            Properties webProperties = (Properties) request.getSession()
                    .getServletContext().getAttribute(Constants.WEB_PROPERTIES);
            if ("true".equalsIgnoreCase(webProperties
                    .getProperty(WEB_SERVICE_DISABLED_PROPERTY))) {
                throw new ServiceForbiddenException("Web service is disabled.");
            }

            execute(request, response);

        } catch (Throwable t) {
            sendError(t, response);
        } 
        output.flush();
    }

    private void sendError(Throwable t, HttpServletResponse response) {
        String msg = WebServiceConstants.SERVICE_FAILED_MSG;
        int code;
        if (t instanceof ServiceException) {
            if (t.getMessage() != null && t.getMessage().length() >= 0) {
                msg = t.getMessage();
            }
            ServiceException ex = (ServiceException) t;
            code = ex.getHttpErrorCode();
        } else {
            code = Output.SC_INTERNAL_SERVER_ERROR;
        }
        logError(t, msg, code);
        sendErrorMsg(response, formatErrorMsg(msg, code), code);
    }

    private void logError(Throwable t, String msg, int code) {
        if (code == Output.SC_INTERNAL_SERVER_ERROR) {
            logger.error("Service failed by internal error. Request parameters: \n" 
                    + requestParametersToString(), t);
        } else {
            logger.debug("Service didn't succeed. It's not an internal error. "
                    + "Reason: " + getErrorDescription(msg, code));
        }
    }
    
    private String requestParametersToString() {
        StringBuilder sb = new StringBuilder();
        Map<String, String[]> map = request.getParameterMap();
        for (String name : map.keySet()) {
            for (String value : map.get(name)) {
                sb.append(name);
                sb.append(": ");
                sb.append(value);
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String getErrorDescription(String msg, int errorCode) {
        StringBuilder sb = new StringBuilder();
        sb.append(StatusDictionary.getDescription(errorCode));
        sb.append(msg);
        return sb.toString();
    }

    private void sendErrorMsg(HttpServletResponse response, String msg, int code) {
        // When status is set, buffer with previous results is cleaned and
        // that's why errors must be set again
        
        
        if (!response.isCommitted()) {
            // Cheating here. It is an xml output, but when content type is set
            // to html, then
            // browsers try to display in more readable way then xml
            response.setContentType("text/html");
            try {
                // Error message is written together with response status code
                // and it is written to the output as well. So it is displayed
                // in browser in case of problems.
                // Used deprecated setStatus method because there isn't any
                // other
                // method for sending error with simple description which
                // wouldn't be formatted
                // by server
                response.setStatus(code, msg);
                if (code != Output.SC_NO_CONTENT) {
                    response.getWriter().print(msg);    
                }
            } catch (IOException e) {
                logger.error("Writing error to response failed.", e);
            }
        } else {
            try {
                response.getWriter().print(msg);
            } catch (IOException e) {
                logger.error("Writing error to response failed.", e);
            }
        }
    }

    private String formatErrorMsg(String content, int errorCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("<error>");
        printMessage(StatusDictionary.getDescription(errorCode), sb);
        printMessage(content, sb);
        sb.append("</error>");
        return sb.toString();
    }

    private void printMessage(String string, StringBuilder sb) {
        sb.append("<message>");
        sb.append(string);
        sb.append("</message>");
    }

    private void initOutput(HttpServletResponse response) {
        PrintWriter out;
        try {
            out = response.getWriter();
        } catch (IOException e) {
            throw new InternalErrorException(e);
        }
        switch (getFormat()) {
        case XML_FORMAT:
            output = new StreamedOutput(out, new XMLFormatter());
            ResponseUtil.setXMLHeader(response, "result.xml");
            break;
        case TSV_FORMAT:
            output = new StreamedOutput(out, new TabFormatter());
            ResponseUtil.setTabHeader(response, "result.tsv");
            break;
        case CSV_FORMAT:
            output = new StreamedOutput(out, new CSVFormatter());
            ResponseUtil.setCSVHeader(response, "result.csv");
            break;
        case HTML_FORMAT:
            output = new HTMLOutput(out);
            ResponseUtil.setHTMLContentType(response);
            break;
        default:
            throw new BadRequestException("Invalid format.");
        }
    }

    /**
     * Returns required output format.
     * 
     * @return format
     */
    public int getFormat() {
        String format = request
                .getParameter(WebServiceRequestParser.OUTPUT_PARAMETER);
        if (format == null || format.equals("")) {
            return TSV_FORMAT;
        } else {
            if (WebServiceRequestParser.FORMAT_PARAMETER_XML
                    .equalsIgnoreCase(format)) {
                return XML_FORMAT;
            }
            if (WebServiceRequestParser.FORMAT_PARAMETER_HTML
                    .equalsIgnoreCase(format)) {
                return HTML_FORMAT;
            } 
            if (WebServiceRequestParser.FORMAT_PARAMETER_CSV
                    .equalsIgnoreCase(format)) {
                return CSV_FORMAT;
            } else {
                return TSV_FORMAT;
            }
        }
    }

    /**
     * Runs service. This is abstract method, that must be defined in subclasses
     * and so performs something useful. Standard procedure is overwrite this
     * method in subclasses and let this method to be called from
     * WebService.doGet method that encapsulates logic common for all web
     * services else you can overwrite doGet method in your web service class
     * and manage all the things alone.
     * 
     * @param request
     *            request
     * @param response
     *            response
     * @throws Exception
     *             if some error occurs
     */
    protected abstract void execute(HttpServletRequest request,
            HttpServletResponse response) throws Exception;

    /**
     * Returns dispatcher that forwards to the page that displays results as a
     * html page.
     * 
     * @return dispatcher
     */
    public RequestDispatcher getHtmlForward() {
        return request.getSession().getServletContext().getRequestDispatcher(
                FORWARD_PATH);
    }
}
