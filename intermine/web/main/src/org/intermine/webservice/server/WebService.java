package org.intermine.webservice.server;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.ProfileManager.ApiPermission;
import org.intermine.api.profile.ProfileManager.AuthenticationException;
import org.intermine.util.PropertiesUtil;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.RequestUtil;
import org.intermine.web.logic.export.Exporter;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.logic.profile.LoginHandler;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;
import org.intermine.webservice.server.output.CSVFormatter;
import org.intermine.webservice.server.output.JSONCountFormatter;
import org.intermine.webservice.server.output.JSONFormatter;
import org.intermine.webservice.server.output.JSONObjectFormatter;
import org.intermine.webservice.server.output.JSONResultFormatter;
import org.intermine.webservice.server.output.JSONRowFormatter;
import org.intermine.webservice.server.output.JSONTableFormatter;
import org.intermine.webservice.server.output.MemoryOutput;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.PlainFormatter;
import org.intermine.webservice.server.output.StreamedOutput;
import org.intermine.webservice.server.output.TabFormatter;
import org.intermine.webservice.server.output.XMLFormatter;

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
 * @author Alex Kalderimis
 * @version
 */
public abstract class WebService
{

    /** Default jsonp callback **/
    public static final String DEFAULT_CALLBACK = "callback";

    /** The format for when no value is given **/
    public static final int EMPTY_FORMAT = -1;

    /** The Unknown format **/
    public static final int UNKNOWN_FORMAT = -2;

    /** XML format constant **/
    public static final int XML_FORMAT = 0;

    /** TSV format constant **/
    public static final int TSV_FORMAT = 1;

    /** HTML format constant **/
    public static final int HTML_FORMAT = 2;

    /** CSV format constant **/
    public static final int CSV_FORMAT = 3;

    /** Count format constant **/
    public static final int COUNT_FORMAT = 4;

    /** Text format constant **/
    public static final int TEXT_FORMAT = 5;

    // FORMAT CONSTANTS BETWEEN 20-40 ARE RESERVED FOR JSON FORMATS!!

    /** Start of JSON format range **/
    public static final int JSON_RANGE_START = 20;

    /** End of JSON format range **/
    public static final int JSON_RANGE_END = 40;

    /** JSONP format constant **/
    public static final int JSON_FORMAT = 20;

    /** JSONP format constant **/
    public static final int JSONP_FORMAT = 21;

    /** JSON Object format constant **/
    public static final int JSON_OBJ_FORMAT = 22;

    /** JSONP Object format constant **/
    public static final int JSONP_OBJ_FORMAT = 23;

    /** JSON Table format constant **/
    public static final int JSON_TABLE_FORMAT = 24;

    /** JSONP Table format constant **/
    public static final int JSONP_TABLE_FORMAT = 25;

    /** JSON Row format constant **/
    public static final int JSON_ROW_FORMAT = 26;

    /** JSONP Row format constant **/
    public static final int JSONP_ROW_FORMAT = 27;

    /** JSON count format constant **/
    public static final int JSON_COUNT_FORMAT = 28;

    /** JSONP count format constant **/
    public static final int JSONP_COUNT_FORMAT = 29;

    /** JSON data table format constant **/
    public static final int JSON_DATA_TABLE_FORMAT = 30;

    /** JSONP data table format constant **/
    public static final int JSONP_DATA_TABLE_FORMAT = 31;

    private static final String COMPRESS = "compress";
    private static final String GZIP = "gzip";
    private static final String ZIP = "zip";
    private static final String WEB_SERVICE_DISABLED_PROPERTY = "webservice.disabled";
    private static final Logger LOG = Logger.getLogger(WebService.class);
    private static final String FORWARD_PATH = "/webservice/table.jsp";
    private static final String AUTHENTICATION_FIELD_NAME = "Authorization";
    private static final String AUTH_TOKEN_PARAM_KEY = "token";
    private static final Profile ANON_PROFILE = new AnonProfile();

    /**
     * Constants for property keys in global property configuration.
     */
    private static final String WS_HEADERS_PREFIX = "ws.response.header";
    private static final String BOTS = "ws.robots";

    protected HttpServletRequest request;
    protected HttpServletResponse response;
    protected Output output;
    protected InterMineAPI im;

    private ApiPermission permission = null;

    /** The properties this mine was configured with **/
    protected Properties webProperties;

    /**
     * Construct the web service with the InterMine API object that gives access
     * to the core InterMine functionality.
     *
     * @param im the InterMine application
     */
    public WebService(InterMineAPI im) {
        this.im = im;
    }

    /**
     * Starting method of web service. The web service should be run like
     *
     * <pre>
     * new ListsService().service(request, response);
     * </pre>
     *
     * Ensures initialisation of web service and makes steps common for all web
     * services and after that executes the <tt>execute</tt> method, that should be
     * overwritten with each web service.
     *
     * @param request The request, as received by the servlet.
     * @param response The response, as handled by the servlet.
     */
    public void service(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
        this.webProperties = SessionMethods.getWebProperties(request);

        if (!agentIsRobot()) {
            try {
                setHeaders();
                initOutput();
                checkEnabled();
                authenticate();
                initState();
                validateState();
                execute();
            } catch (Throwable t) {
                sendError(t, response);
            }
        } else {
            response.setStatus(403);
        }

        try {
            if (output != null) {
                output.flush();
            }
        } catch (Throwable t) {
            logError(t, "Error flushing", 500);
        }

        try {
            cleanUp();
        } catch (Throwable t) {
            LOG.error("Error cleaning up", t);
        }
        // Do not persist sessions. All requests should be state-less.
        request.getSession().invalidate();

    }

    private boolean agentIsRobot() {
        String ua = request.getHeader("User-Agent");
        if (ua != null) {
            ua = ua.toLowerCase();
            String[] robots = StringUtils.split(webProperties.getProperty(BOTS, ""), ',');
            for (String bot : robots) {
                if (ua.contains(bot.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void setHeaders() {
        Properties headerProps
            = PropertiesUtil.getPropertiesStartingWith(WS_HEADERS_PREFIX, webProperties);
        for (Object o : headerProps.values()) {
            String h = o.toString();
            String[] parts = StringUtils.split(h, ":", 2);
            if (parts.length != 2) {
                LOG.warn("Ignoring invalid response header: " + h);
            } else {
                response.setHeader(parts[0].trim(), parts[1].trim());
            }
        }
    }

    private void checkEnabled() {
        Properties webProperties = SessionMethods.getWebProperties(request
                .getSession().getServletContext());
        if ("true".equalsIgnoreCase(webProperties
                .getProperty(WEB_SERVICE_DISABLED_PROPERTY))) {
            throw new ServiceForbiddenException("Web service is disabled.");
        }
    }

    /**
     * Subclasses may put clean-up code here, to be run after the request has been executed.
     */
    protected void cleanUp() {
        // No-op stub.
    }

    /**
     * Subclasses can put initialisation here.
     */
    protected void initState() {
        // No-op stub
    }

    /**
     * Subclasses can put initialisation checks here.
     * The main use case is for confirming
     * authentication.
     */
    protected void validateState() {
        // No-op stub
    }

    /**
     * If user name and password is specified in request, then it setups user
     * profile in session. User was authenticated. It uses HTTP basic access
     * authentication.
     * {@link "http://en.wikipedia.org/wiki/Basic_access_authentication"}
     */
    private void authenticate() {

        final String authToken = request.getParameter(AUTH_TOKEN_PARAM_KEY);
        final ProfileManager pm = im.getProfileManager();
        final HttpSession session = request.getSession();
        // Anonymous requests get the anonymous profile.
        SessionMethods.setProfile(session, ANON_PROFILE);

        try {
            if (StringUtils.isEmpty(authToken)) {
                final String authString = request.getHeader(AUTHENTICATION_FIELD_NAME);
                if (StringUtils.isEmpty(authString) || formatIsJSONP()) {
                    return; // Not Authenticated.
                }

                // Strip off the "Basic" part - but don't require it.
                final String encoded = StringUtils.removeStart(authString, "Basic ");
                final String decoded = new String(Base64.decodeBase64(encoded.getBytes()));
                final String[] parts = decoded.split(":", 2);
                if (parts.length != 2) {
                    throw new BadRequestException(
                        "Invalid request authentication. "
                        + "Authorization field contains invalid value. "
                        + "Decoded authorization value: " + parts[0]);
                }
                final String username = parts[0];
                final String password = parts[1];

                permission = pm.getPermission(username, password, im.getClassKeys());
            } else {
                permission = pm.getPermission(authToken, im.getClassKeys());
            }
        } catch (AuthenticationException e) {
            throw new ServiceForbiddenException(e.getMessage(), e);
        }

        LoginHandler.setUpProfile(session, permission.getProfile());
    }

    private void sendError(Throwable t, HttpServletResponse response) {
        String msg = WebServiceConstants.SERVICE_FAILED_MSG;
        if (t.getMessage() != null && t.getMessage().length() >= 0) {
            msg = t.getMessage();
        }
        int code;
        if (t instanceof ServiceException) {

            ServiceException ex = (ServiceException) t;
            code = ex.getHttpErrorCode();
        } else {
            code = Output.SC_INTERNAL_SERVER_ERROR;
        }
        logError(t, msg, code);
        if (!formatIsJSONP()) {
            // Don't set errors statuses on jsonp requests, to enable
            // better error checking in the browser.
            response.setStatus(code);
        } else {
            // But do set callbacks
            String callback = getCallback();
            if (callback == null) {
                callback = "makeInterMineResultsTable";
            }
            Map<String, Object> attributes = new HashMap<String, Object>();
            attributes.put(JSONResultFormatter.KEY_CALLBACK, callback);
            output.setHeaderAttributes(attributes);
        }
        output.setError(msg, code);
        LOG.debug("Set error to : " + msg + "," + code);
    }

    private void logError(Throwable t, String msg, int code) {

        // Stack traces for all!
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(b);
        t.printStackTrace(ps);
        ps.flush();

        if (code == Output.SC_INTERNAL_SERVER_ERROR) {
            LOG.debug("Service failed by internal error. Request parameters: \n"
                            + requestParametersToString() + b.toString());
        } else {
            LOG.debug("Service didn't succeed. It's not an internal error. "
                    + "Reason: " + getErrorDescription(msg, code) + "\n" + b.toString());
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


    /**
     * @return Whether or not the requested result format is one of our JSON formats.
     */
    protected boolean formatIsJSON() {
        int format = getFormat();
        return (format >= JSON_RANGE_START && format <= JSON_RANGE_END);
    }

    /**
     * @return Whether or not the format is a JSON-P format
     */
    protected boolean formatIsJSONP() {
        return formatIsJSON() && (getFormat() % 2 == 1);
    }

    /**
     * @return Whether or not the format is for JSON-Objects
     */
    protected boolean formatIsJsonObj() {
        int format = getFormat();
        return (format == JSON_OBJ_FORMAT || format == JSONP_OBJ_FORMAT);
    }

    /**
     * @return Whether or not the format is a flat-file format
     */
    protected boolean formatIsFlatFile() {
        int format = getFormat();
        return (format == TSV_FORMAT || format == CSV_FORMAT);
    }

    /**
     * Returns true if the format requires the count, rather than the full or
     * paged result set.
     * @return a truth value
     */
    public boolean formatIsCount() {
        int format = getFormat();
        switch (format) {
            case COUNT_FORMAT:
                return true;
            case JSONP_COUNT_FORMAT:
                return true;
            case JSON_COUNT_FORMAT:
                return true;
            default:
                return false;
        }
    }

    /**
     * @return Whether or not the format is XML.
     */
    public boolean formatIsXML() {
        return (getFormat() == XML_FORMAT);
    }

    /**
     * Make the XML output given the HttpResponse's PrintWriter.
     * @param out The PrintWriter from the HttpResponse.
     * @return An Output that produces good XML.
     */
    protected Output makeXMLOutput(PrintWriter out, String separator) {
        ResponseUtil.setXMLHeader(response, "result.xml");
        return new StreamedOutput(out, new XMLFormatter(), separator);
    }

    /**
     * Make the default JSON output given the HttpResponse's PrintWriter.
     * @param out The PrintWriter from the HttpResponse.
     * @return An Output that produces good JSON.
     */
    protected Output makeJSONOutput(PrintWriter out, String separator) {
        return new StreamedOutput(out, new JSONFormatter(), separator);
    }


    /**
     * @return Whether or not this request wants gzipped data.
     */
    protected boolean isGzip() {
        return GZIP.equalsIgnoreCase(request.getParameter(COMPRESS));
    }

    /**
     * @return Whether or not this request wants zipped data.
     */
    protected boolean isZip() {
        return ZIP.equalsIgnoreCase(request.getParameter(COMPRESS));
    }

    /**
     * @return Whether or not this request wants uncompressed data.
     */
    protected boolean isUncompressed() {
        return StringUtils.isEmpty(request.getParameter(COMPRESS));
    }

    /**
     * @return the file-name extension for the result-set.
     */
    protected String getExtension() {
        if (isGzip()) {
            return ".gz";
        } else if (isZip()) {
            return ".zip";
        } else {
            return "";
        }
    }

    private void initOutput() {
        final String separator;
        if (RequestUtil.isWindowsClient(request)) {
            separator = Exporter.WINDOWS_SEPARATOR;
        } else {
            separator = Exporter.UNIX_SEPARATOR;
        }
        int format = getFormat();

        PrintWriter out;
        OutputStream os;
        try {
            // set reasonable buffer size
            response.setBufferSize(8 * 1024);
            os = response.getOutputStream();
            if (isGzip()) {
                os = new GZIPOutputStream(os);
            } else if (isZip()) {
                os = new ZipOutputStream(new BufferedOutputStream(os));
            }
            out = new PrintWriter(os);
        } catch (IOException e) {
            throw new InternalErrorException(e);
        }

        String filename = getDefaultFileName();
        switch (format) {
        // HTML is a special case
            case HTML_FORMAT:
                output = new StreamedOutput(out, new PlainFormatter(), separator);
                ResponseUtil.setHTMLContentType(response);
                break;
            case XML_FORMAT:
                output = makeXMLOutput(out, separator);
                break;
            case TSV_FORMAT:
                output = new StreamedOutput(out, new TabFormatter(), separator);
                filename = "result.tsv";
                if (isUncompressed()) {
                    ResponseUtil.setTabHeader(response, filename);
                }
                break;
            case CSV_FORMAT:
                output = new StreamedOutput(out, new CSVFormatter(), separator);
                filename = "result.csv";
                if (isUncompressed()) {
                    ResponseUtil.setCSVHeader(response, filename);
                }
                break;
            case COUNT_FORMAT:
                output = new StreamedOutput(out, new PlainFormatter(), separator);
                filename = "count.txt";
                if (isUncompressed()) {
                    ResponseUtil.setPlainTextHeader(response, filename);
                }
                break;
            case TEXT_FORMAT:
                output = new StreamedOutput(out, new PlainFormatter(), separator);
                filename = "result.txt";
                if (isUncompressed()) {
                    ResponseUtil.setPlainTextHeader(response, filename);
                }
                break;
            case JSON_FORMAT:
                output = makeJSONOutput(out, separator);
                filename = "result.json";
                if (isUncompressed()) {
                    ResponseUtil.setJSONHeader(response, filename);
                }
                break;
            case JSONP_FORMAT:
                output = makeJSONOutput(out, separator);
                filename = "result.jsonp";
                if (isUncompressed()) {
                    ResponseUtil.setJSONPHeader(response, filename);
                }
                break;
            case JSON_OBJ_FORMAT:
                output = new StreamedOutput(out, new JSONObjectFormatter(), separator);
                filename = "result.json";
                if (isUncompressed()) {
                    ResponseUtil.setJSONHeader(response, filename);
                }
                break;
            case JSONP_OBJ_FORMAT:
                output = new StreamedOutput(out, new JSONObjectFormatter(), separator);
                filename = "result.jsonp";
                if (isUncompressed()) {
                    ResponseUtil.setJSONPHeader(response, filename);
                }
                break;
            case JSON_TABLE_FORMAT:
                output = new StreamedOutput(out, new JSONTableFormatter(), separator);
                filename = "resulttable.json";
                if (isUncompressed()) {
                    ResponseUtil.setJSONHeader(response, filename);
                }
                break;
            case JSONP_TABLE_FORMAT:
                output = new StreamedOutput(out, new JSONTableFormatter(), separator);
                filename = "resulttable.jsonp";
                if (isUncompressed()) {
                    ResponseUtil.setJSONPHeader(response, filename);
                }
                break;
            case JSON_DATA_TABLE_FORMAT:
                output = new StreamedOutput(out, new JSONTableFormatter(), separator);
                filename = "resulttable.json";
                if (isUncompressed()) {
                    ResponseUtil.setJSONHeader(response, filename);
                }
                break;
            case JSONP_DATA_TABLE_FORMAT:
                output = new StreamedOutput(out, new JSONTableFormatter(), separator);
                filename = "resulttable.jsonp";
                if (isUncompressed()) {
                    ResponseUtil.setJSONPHeader(response, filename);
                }
                break;
            case JSON_ROW_FORMAT:
                output = new StreamedOutput(out, new JSONRowFormatter(), separator);
                ResponseUtil.setJSONHeader(response,
                        "result.json" + getExtension());
                break;
            case JSONP_ROW_FORMAT:
                output = new StreamedOutput(out, new JSONRowFormatter(), separator);
                ResponseUtil.setJSONPHeader(response,
                        "result.json" + getExtension());
                break;
            case JSON_COUNT_FORMAT:
                output = new StreamedOutput(out, new JSONCountFormatter(), separator);
                filename = "resultcount.json";
                if (isUncompressed()) {
                    ResponseUtil.setJSONHeader(response, filename);
                }
                break;
            case JSONP_COUNT_FORMAT:
                output = new StreamedOutput(out, new JSONCountFormatter(), separator);
                filename = "resultcount.jsonp";
                if (isUncompressed()) {
                    ResponseUtil.setJSONPHeader(response, filename);
                }
                break;
            default:
                output = getDefaultOutput(out, os, separator);
        }
        if (!isUncompressed()) {
            filename += getExtension();
            ResponseUtil.setGzippedHeader(response, filename);
            if (isZip()) {
                try {
                    ((ZipOutputStream) os).putNextEntry(new ZipEntry(filename));
                } catch (IOException e) {
                    throw new InternalErrorException(e);
                }
            }
        }
    }

    /**
     * @return The default file name for this service. (default = "result.tsv")
     */
    protected String getDefaultFileName() {
        return "result.tsv";
    }

    /**
     * Make the default output for this service.
     * @param out The response's PrintWriter.
     * @param os The Response's output stream.
     * @return An Output. (default = new StreamedOutput(out, new TabFormatter()))
     */
    protected Output getDefaultOutput(PrintWriter out, OutputStream os, String separator) {
        output = new StreamedOutput(out, new TabFormatter(), separator);
        ResponseUtil.setTabHeader(response, getDefaultFileName());
        return output;
    }

    /**
     * Returns true if the request wants column headers as well as result rows
     * @return true if the request declares it wants column headers
     */
    public boolean wantsColumnHeaders() {
        String wantsCols = request.getParameter(WebServiceRequestParser.ADD_HEADER_PARAMETER);
        boolean no = (wantsCols == null || wantsCols.isEmpty() || "0".equals(wantsCols));
        return !no;
    }

    /**
     * Get an enum which represents the column header style (path, friendly, or none)
     * @return a column header style
     */
    public ColumnHeaderStyle getColumnHeaderStyle() {
        if (wantsColumnHeaders()) {
            String style = request.getParameter(WebServiceRequestParser.ADD_HEADER_PARAMETER);
            if ("path".equalsIgnoreCase(style)) {
                return ColumnHeaderStyle.PATH;
            } else {
                return ColumnHeaderStyle.FRIENDLY;
            }
        } else {
            return ColumnHeaderStyle.NONE;
        }
    }

    /**
     * Parse a format from the path-info of the request.
     * By default, if the path-info is one of "xml", "json", "jsonp", "tsv" or "csv",
     * then an appropriate format will be returned. All other values will cause
     * null to be returned.
     * @return A format string.
     */
    protected String parseFormatFromPathInfo() {
        String pathInfo = request.getPathInfo();
        pathInfo = StringUtil.trimSlashes(pathInfo);
        if ("xml".equalsIgnoreCase(pathInfo)) {
            return WebServiceRequestParser.FORMAT_PARAMETER_XML;
        } else if ("json".equalsIgnoreCase(pathInfo)) {
            return WebServiceRequestParser.FORMAT_PARAMETER_JSON;
        } else if ("jsonp".equalsIgnoreCase(pathInfo)) {
            return WebServiceRequestParser.FORMAT_PARAMETER_JSONP;
        } else if ("tsv".equalsIgnoreCase(pathInfo)) {
            return WebServiceRequestParser.FORMAT_PARAMETER_TAB;
        } else if ("csv".equalsIgnoreCase(pathInfo)) {
            return WebServiceRequestParser.FORMAT_PARAMETER_CSV;
        }
        return null;
    }

    /**
     * @return The default format constant for this service.
     */
    protected int getDefaultFormat() {
        return EMPTY_FORMAT;
    }

    /**
     * Returns required output format.
     *
     * @return format
     */
    public int getFormat() {
        String format;
        if (request.getPathInfo() != null) {
            format = parseFormatFromPathInfo();
        } else {
            format = request.getParameter(WebServiceRequestParser.OUTPUT_PARAMETER);
        }
        if (StringUtils.isEmpty(format)) {
            return getDefaultFormat();
        }
        if (WebServiceRequestParser.FORMAT_PARAMETER_XML
                .equalsIgnoreCase(format)) {
            return XML_FORMAT;
        }
        if (WebServiceRequestParser.FORMAT_PARAMETER_HTML
                .equalsIgnoreCase(format)) {
            return HTML_FORMAT;
        }
        if (WebServiceRequestParser.FORMAT_PARAMETER_TAB
                .equalsIgnoreCase(format)) {
            return TSV_FORMAT;
        }
        if (WebServiceRequestParser.FORMAT_PARAMETER_CSV
                .equalsIgnoreCase(format)) {
            return CSV_FORMAT;
        }
        if (WebServiceRequestParser.FORMAT_PARAMETER_COUNT
                .equalsIgnoreCase(format)) {
            return COUNT_FORMAT;
        }
        if (WebServiceRequestParser.FORMAT_PARAMETER_JSON_OBJ
                .equalsIgnoreCase(format)) {
            return JSON_OBJ_FORMAT;
        }
        if (WebServiceRequestParser.FORMAT_PARAMETER_JSONP_OBJ
                .equalsIgnoreCase(format)) {
            return JSONP_OBJ_FORMAT;
        }
        if (WebServiceRequestParser.FORMAT_PARAMETER_JSON_TABLE
                .equalsIgnoreCase(format)) {
            return JSON_TABLE_FORMAT;
        }
        if (WebServiceRequestParser.FORMAT_PARAMETER_JSONP_TABLE
                .equalsIgnoreCase(format)) {
            return JSONP_TABLE_FORMAT;
        }
        if (WebServiceRequestParser.FORMAT_PARAMETER_JSON_ROW
                .equalsIgnoreCase(format)) {
            return JSON_ROW_FORMAT;
        }
        if (WebServiceRequestParser.FORMAT_PARAMETER_JSONP_ROW
                .equalsIgnoreCase(format)) {
            return JSONP_ROW_FORMAT;
        }
        if (WebServiceRequestParser.FORMAT_PARAMETER_JSONP
                .equalsIgnoreCase(format)) {
            return JSONP_FORMAT;
        }
        if (WebServiceRequestParser.FORMAT_PARAMETER_JSON
                .equalsIgnoreCase(format)) {
            return JSON_FORMAT;
        }
        if (WebServiceRequestParser.FORMAT_PARAMETER_JSON_DATA_TABLE
                .equalsIgnoreCase(format)) {
            return JSON_DATA_TABLE_FORMAT;
        }
        if (WebServiceRequestParser.FORMAT_PARAMETER_JSONP_DATA_TABLE
                .equalsIgnoreCase(format)) {
            return JSONP_DATA_TABLE_FORMAT;
        }
        if (WebServiceRequestParser.FORMAT_PARAMETER_JSONP_COUNT
                .equalsIgnoreCase(format)) {
            return JSONP_COUNT_FORMAT;
        }
        if (WebServiceRequestParser.FORMAT_PARAMETER_JSON_COUNT
                .equalsIgnoreCase(format)) {
            return JSON_COUNT_FORMAT;
        }
        return getDefaultFormat();
    }

    /**
     * Get the value of the callback parameter.
     * @return The value, or null if this request type does not support this.
     */
    public String getCallback() {
        if (formatIsJSONP()) {
            if (!hasCallback()) {
                return DEFAULT_CALLBACK;
            } else {
                return request.getParameter(
                        WebServiceRequestParser.CALLBACK_PARAMETER);
            }
        } else {
            return null;
        }
    }

    /**
      * Determine whether a callback was supplied to this request.
      * @return Whether or not a callback was supplied.
      */
    public boolean hasCallback() {
        String cb = request.getParameter(
                WebServiceRequestParser.CALLBACK_PARAMETER);
        return (cb != null && !"".equals(cb));
    }

    /**
     * Runs service. This is abstract method, that must be defined in subclasses
     * and so performs something useful. Standard procedure is overwrite this
     * method in subclasses and let this method to be called from
     * WebService.doGet method that encapsulates logic common for all web
     * services else you can overwrite doGet method in your web service class
     * and manage all the things alone.
     *
     * @throws Exception if some error occurs
     */
    protected abstract void execute() throws Exception;

    /**
     * Returns dispatcher that forwards to the page that displays results as a
     * html page.
     *
     * @return dispatcher
     */
    public RequestDispatcher getHtmlForward() {
        return request.getSession().getServletContext()
                .getRequestDispatcher(FORWARD_PATH);
    }

    /**
     * @return true if request specified user name and password
     */
    public boolean isAuthenticated() {
        return permission != null;
    }
}
