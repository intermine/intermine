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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.ProfileManager.ApiPermission;
import org.intermine.api.profile.ProfileManager.AuthenticationException;
import org.intermine.api.util.AnonProfile;
import org.intermine.util.PropertiesUtil;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.RequestUtil;
import org.intermine.web.logic.export.Exporter;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.logic.profile.LoginHandler;
import org.intermine.web.security.KeyStorePublicKeySource;
import org.intermine.web.security.PublicKeySource;
import org.intermine.webservice.server.core.ListManager;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.MissingParameterException;
import org.intermine.webservice.server.exceptions.NotAcceptableException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;
import org.intermine.webservice.server.exceptions.UnauthorizedException;
import org.intermine.webservice.server.output.CSVFormatter;
import org.intermine.webservice.server.output.HTMLTableFormatter;
import org.intermine.webservice.server.output.JSONFormatter;
import org.intermine.webservice.server.output.JSONObjectFormatter;
import org.intermine.webservice.server.output.JSONResultFormatter;
import org.intermine.webservice.server.output.JSONRowFormatter;
import org.intermine.webservice.server.output.JSONTableFormatter;
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

    private static final String COMPRESS = "compress";
    private static final String GZIP = "gzip";
    private static final String ZIP = "zip";

    private static final Logger LOG = Logger.getLogger(WebService.class);
    private static final String AUTHENTICATION_FIELD_NAME = "Authorization";
    private static final String AUTH_TOKEN_PARAM_KEY = "token";
    private static final Profile ANON_PROFILE = new AnonProfile();

    /**
     * Constants for property keys in global property configuration.
     */
    private static final String WS_HEADERS_PREFIX = "ws.response.header";
    private static final String BOTS = "ws.robots";
    private static final String WEB_SERVICE_DISABLED_PROPERTY = "webservice.disabled";

    /**
     * The servlet request.
     */
    protected HttpServletRequest request;

    /**
     * The servlet response.
     */
    protected HttpServletResponse response;

    /**
     * The response to the outside world.
     */
    protected Output output;

    /**
     * The configuration object.
     */
    protected final InterMineAPI im;

    /** The properties this mine was configured with **/
    protected final Properties webProperties = InterMineContext.getWebProperties();

    private ApiPermission permission = ProfileManager.getDefaultPermission(ANON_PROFILE);
    private boolean initialised = false;
    private String propertyNameSpace = null;

    /**
     * Return the permission object representing the authorisation state of the
     * request. This is guaranteed to not be null.
     *
     * @return A permission object, from which a service may inspect the level
     *         of authorisation, and retrieve details about whom the request is
     *         authorised for.
     */
    protected ApiPermission getPermission() {
        if (permission == null) {
            throw new IllegalStateException(
                    "There should always be a valid permission object");
        }
        return permission;
    }

    /**
     * Get a parameter this service deems to be required.
     *
     * @param name The name of the parameter
     * @return The value of the parameter. Never null, never blank.
     * @throws MissingParameterException
     *             If the value of the parameter is blank or null.
     */
    protected String getRequiredParameter(String name) {
        String value = request.getParameter(name);
        if (StringUtils.isBlank(value)) {
            throw new MissingParameterException(name);
        }
        return value;
    }

    /**
     * Get a parameter this service deems to be optional, or the default value.
     *
     * @param name
     *            The name of the parameter.
     * @param defaultValue
     *            The default value.
     * @return The value provided, if there is a non-blank one, or the default
     *         value.
     */
    protected String getOptionalParameter(String name, String defaultValue) {
        String value = request.getParameter(name);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        return value;
    }

    /**
     * Get a profile that is a true authenticated user that exists in the
     * database.
     *
     * @return The user's profile.
     * @throws ServiceForbiddenException
     *             if this request resolves to an unauthenticated profile.
     */
    protected Profile getAuthenticatedUser() {
        Profile profile = getPermission().getProfile();
        if (profile.isLoggedIn()) {
            return profile;
        }
        throw new ServiceForbiddenException("You must be logged in to use this service");
    }

    /** @return A ListManager for this user. **/
    protected ListManager getListManager() {
        return new ListManager(im, getPermission().getProfile());
    }

    /**
     * Get a parameter this service deems to be optional, or <code>null</code>.
     *
     * @param name
     *            The name of the parameter.
     * @return The value of the parameter, or <code>null</code>
     */
    protected String getOptionalParameter(String name) {
        return getOptionalParameter(name, null);
    }

    /**
     * Get the value of a parameter that should be interpreted as an integer.
     *
     * @param name The name of the parameter.
     * @return An integer
     * @throws BadRequestException if The value is absent or mal-formed.
     */
    protected Integer getIntParameter(String name) {
        String value = getRequiredParameter(name);
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            String msg = String.format("%s should be a valid number. Got %s", name, value);
            throw new BadRequestException(msg, e);
        }
    }

    /**
     * Get the value of a parameter that should be interpreted as an integer.
     *
     * @param name The name of the parameter.
     * @param defaultValue The value to return if none is provided by the user.
     * @return An integer
     * @throw BadRequestException if the user provided a mal-formed value.
     */
    protected Integer getIntParameter(String name, Integer defaultValue) {
        try {
            return getIntParameter(name);
        } catch (MissingParameterException e) {
            return defaultValue;
        }
    }

    /**
     * Set the default name-space for configuration property look-ups.
     *
     * If a value is set, it must be provided before any actions are taken. This
     * means this property must be set before the execute method is called.
     *
     * @param namespace
     *            The name space to use (eg: "some.namespace"). May not be null.
     */
    protected void setNameSpace(String namespace) {
        if (namespace == null || namespace.endsWith(".")) {
            throw new IllegalArgumentException(
                    "Namespace must be a non-null string, and may "
                            + "not terminate in a period. Value was: "
                            + namespace);
        }
        if (initialised) {
            throw new IllegalStateException(
                    "Name space must be set prior to, or as part of, "
                            + "initialisation.");
        }

        propertyNameSpace = namespace;
    }

    /**
     * Get a configuration property by name.
     *
     * @param name
     *            The name of the property to retrieve.
     * @return A configuration value.
     */
    protected String getProperty(String name) {
        if (StringUtils.contains(name, '.')) {
            return webProperties.getProperty(name);
        }
        return webProperties.getProperty(propertyNameSpace == null ? name
                : propertyNameSpace + "." + name);
    }

    /**
     * Construct the web service with the InterMine API object that gives access
     * to the core InterMine functionality.
     *
     * @param im
     *            the InterMine application
     */
    public WebService(InterMineAPI im) {
        this.im = im;
    }

    // TODO:
    // Change the API to:
    // new WebService(Req, Resp)
    // Get rid of servlets - move to single dispatcher.

    /**
     * Starting method of web service. The web service should be run like
     *
     * <pre>
     * new ListsService().service(request, response);
     * </pre>
     *
     * Ensures initialisation of web service and makes steps common for all web
     * services and after that executes the <tt>execute</tt> method, for which
     * each subclass must provide an implementation.
     *
     * @param request
     *            The request, as received by the servlet.
     * @param response
     *            The response, as handled by the servlet.
     */
    public void service(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;

        try {
            if (agentIsRobot()) {
                response.sendError(Output.SC_FORBIDDEN);
            } else {
                setHeaders();
                initState();
                initOutput();
                checkEnabled();
                authenticate();
                initialised = true;
                postInit();
                validateState();
                execute();
            }
        } catch (Throwable t) {
            sendError(t, response);
        }

        try {
            if (output == null) {
                response.flushBuffer();
            } else {
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

    }

    private boolean agentIsRobot() {
        String ua = request.getHeader("User-Agent");
        if (ua != null) {
            ua = ua.toLowerCase();
            String[] robots = StringUtils.split(
                    webProperties.getProperty(BOTS, ""), ',');
            for (String bot : robots) {
                if (ua.contains(bot.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void setHeaders() {
        Properties headerProps = PropertiesUtil.getPropertiesStartingWith(
                WS_HEADERS_PREFIX, webProperties);

        for (Object o : headerProps.values()) {
            String h = o.toString();
            String[] parts = StringUtils.split(h, ":", 2);
            if (parts.length != 2) {
                LOG.warn("Ignoring invalid response header: " + h);
            } else {
                response.setHeader(parts[0].trim(), parts[1].trim());
            }
        }

        String origin = request.getHeader("Origin");
        if (StringUtils.isNotBlank(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        }
    }

    private void checkEnabled() {
        if ("true".equalsIgnoreCase(webProperties
                .getProperty(WEB_SERVICE_DISABLED_PROPERTY))) {
            throw new ServiceForbiddenException("Web service is disabled.");
        }
    }

    /**
     * Subclasses may put clean-up code here, to be run after the request has
     * been executed.
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
     * Subclasses can put initialisation checks here. The main use case is for
     * confirming authentication.
     */
    protected void validateState() {
        // No-op stub
    }

    /**
     * Subclasses can hook in here to do common behaviour that needs to happen
     * after initialisation.
     */
    protected void postInit() {
        // No-op stub;
    }

    private JWTVerifier.Verification getIdentityFromBearerToken(final String rawString) {
        JWTVerifier verifier;
        PublicKeySource keys;

        try {
            keys = new KeyStorePublicKeySource(InterMineContext.getKeyStore());
        } catch (KeyStoreException e) {
            throw new ServiceException("Failed to load key store.", e);
        } catch (NoSuchAlgorithmException e) {
            throw new ServiceException("Key store incorrectly configured", e);
        } catch (CertificateException e) {
            throw new ServiceException("Key store incorrectly configured", e);
        } catch (IOException e) {
            throw new ServiceException("Failed to load key store.", e);
        }
        try {
            verifier = new JWTVerifier(keys, webProperties);
            return verifier.verify(rawString);
        } catch (JWTVerifier.VerificationError e) {
            throw new UnauthorizedException(e.getMessage());
        }
    }

    private String getIdentityAssertion() {
        String header = webProperties.getProperty("authentication.identity.assertion.header");

        if (StringUtils.isNotBlank(header)) {
            return request.getHeader(header);
        }
        return null;
    }

    /**
     * This method is responsible for setting the Permission for the current
     * request. It can be derived in a number of ways:
     *
     * <ul>
     *   <li>
     *     <h4>Basic Authentication</h3>
     *     Standard username and password stuff - best avoided.
     *   </li>
     *   <li>
     *     <h4>Token authentication</h4>
     *     User passes back an opaque token which has no meaning outside of this
     *     application. Recommended. The token can be either passed as the value of the
     *     <code>token</code> query parameter, or provided in the
     *     <code>Authorization</code> header with the string <code>"Token "</code>
     *     preceding it, i.e.:
     *     <code>Authorization: Token somelongtokenstring</code>
     *   </li>
     *   <li>
     *     <h4>JWT bearer tokens</h4>
     *     The user passes back a bearer token issued by someone we trust (could
     *     include ourselves). This requires the configuration of a keystore
     *     {@see KeyStoreBuilder}. Provides delegated authentication capabilities.
     *     Overkill for most users. The token must be provided in the <code>Authorization</code>
     *     header, preceded by the string <code>"Bearer "</code>, e.g.:
     *     <code>Authorization: Bearer yourjwttokenhere</code>
     *   </li>
     * </ul>
     *
     * {@link "http://en.wikipedia.org/wiki/Basic_access_authentication"}
     * {@link "http://jwt.io/"}
     */
    private void authenticate() {

        String authToken = request.getParameter(AUTH_TOKEN_PARAM_KEY);
        JWTVerifier.Verification identity = null;
        final String authString = request.getHeader(AUTHENTICATION_FIELD_NAME);
        final ProfileManager pm = im.getProfileManager();

        if (StringUtils.isEmpty(authToken) && StringUtils.isEmpty(authString)) {
            return; // Not Authenticated.
        }
        // Accept tokens passed in the Authorization header.
        if (StringUtils.isEmpty(authToken)) {
            if (StringUtils.startsWith(authString, "Token ")) {
                authToken = StringUtils.removeStart(authString, "Token ");
                try { // Allow bearer tokens to be passed in as normal tokens.
                    identity = getIdentityFromBearerToken(authToken);
                } catch (UnauthorizedException e) {
                    // pass - check the token below.
                }
            } else if (StringUtils.startsWith(authString, "Bearer ")) {
                identity = getIdentityFromBearerToken(
                    StringUtils.removeStart(authString, "Bearer "));
            } else {
                String identityAssertion = getIdentityAssertion();
                if (StringUtils.isNotBlank(identityAssertion)) {
                    identity = getIdentityFromBearerToken(identityAssertion);
                }
            }
        }

        try {
            // Use a token if provided.
            if (identity != null) {
                permission = pm.grantPermission(
                     identity.getIssuer(),
                     identity.getIdentity(),
                     im.getClassKeys());
            } else if (StringUtils.isNotEmpty(authToken)) {
                permission = pm.getPermission(authToken, im.getClassKeys());
            } else {
                 // Try and read the authString as a basic auth header.
                 // Strip off the "Basic" part - but don't require it.
                final String encoded = StringUtils.removeStart(authString, "Basic ");
                final String decoded = new String(Base64.decodeBase64(encoded.getBytes()));
                final String[] parts = decoded.split(":", 2);
                if (parts.length != 2) {
                    throw new UnauthorizedException(
                             "Invalid request authentication. "
                                     + "Authorization field contains invalid value. "
                                     + "Decoded authorization value: "
                                     + parts[0]);
                }
                 // Allow tokens to be passed in basic auth headers.
                if (StringUtils.isEmpty(parts[1])) {
                    permission = pm.getPermission(parts[0], im.getClassKeys());
                } else {
                    final String username = StringUtils.lowerCase(parts[0]);
                    final String password = parts[1];

                    permission = pm.getPermission(username, password, im.getClassKeys());
                }
            }
        } catch (AuthenticationException e) {
            throw new UnauthorizedException(e.getMessage());
        }

        LoginHandler.setUpPermission(im, permission);
    }

    private void sendError(Throwable t, HttpServletResponse response) {

        String msg = WebServiceConstants.SERVICE_FAILED_MSG;
        boolean showAllMsgs = webProperties.containsKey("i.am.a.dev");

        int code;
        if (t instanceof ServiceException) {
            code = ((ServiceException) t).getHttpErrorCode();
        } else {
            code = Output.SC_INTERNAL_SERVER_ERROR;
        }
        String realMsg = t.getMessage();
        if ((showAllMsgs || code < 500) && !StringUtils.isBlank(realMsg)) {
            msg = realMsg;
        }
        logError(t, realMsg, code);
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
        if (output != null) {
            output.setError(msg, code);
            LOG.debug("Set error to : " + msg + "," + code);
        }
    }

    private void logError(Throwable t, String msg, int code) {

        // Stack traces for all!
        String truncatedStackTrace = getTruncatedStackTrace(t);

        if (code == Output.SC_INTERNAL_SERVER_ERROR) {
            LOG.error("Service failed by internal error. Request parameters: \n"
                    + requestParametersToString() + t + "\n" + truncatedStackTrace);
        } else {
            LOG.debug("Service didn't succeed. It's not an internal error. "
                    + "Reason: " + getErrorDescription(msg, code) + "\n"
                    + truncatedStackTrace);
        }
    }

    private String getTruncatedStackTrace(Throwable t) {
        StackTraceElement[] stack = t.getStackTrace();
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(b);
        boolean tooDeep = false;

        for (int i = 0; !tooDeep && i < stack.length; i++) {
            StackTraceElement element = stack[i];
            if (element.getClassName().contains("catalina")) {
                // We have descended as far as is useful. stop here.
                tooDeep = true;
                ps.print("\n ...");
            } else {
                ps.print("\n  at ");
                ps.print(element);
            }
        }
        if (t.getCause() != null) {
            ps.print("\n caused by: " + t.getCause() + "\n" + getTruncatedStackTrace(t.getCause()));
        }
        ps.flush();
        return b.toString();
    }

    private String requestParametersToString() {
        StringBuilder sb = new StringBuilder();
        @SuppressWarnings("unchecked") // Old pre-generic API.
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
        sb.append(" ");
        sb.append(msg);
        return sb.toString();
    }

    /**
     * @return Whether or not the requested result format is one of our JSON
     *         formats.
     */
    protected final boolean formatIsJSON() {
        return Format.JSON_FORMATS.contains(getFormat());
    }

    /**
     * @return Whether or not the format is a JSON-P format
     */
    protected final boolean formatIsJSONP() {
        if (isJsonP == null) {
            isJsonP = WebServiceRequestParser.isJsonP(request);
        }
        return isJsonP;
    }

    /**
     * @return Whether or not the format is a flat-file format
     */
    protected final boolean formatIsFlatFile() {
        return Format.FLAT_FILES.contains(getFormat());
    }

    /**
     * Returns true if the format requires the count, rather than the full or
     * paged result set.
     *
     * @return a truth value
     */
    // This should not be in the general case.
    //public boolean formatIsCount() {
    //    int format = getFormat();
    //    return (format == Formats.COUNT || format == Formats.JSON_COUNT);
    //}

    /**
     * @return Whether or not the format is XML.
     */
    public boolean formatIsXML() {
        return (getFormat() == Format.XML);
    }

    /**
     * Make the XML output given the HttpResponse's PrintWriter.
     *
     * @param out The PrintWriter from the HttpResponse.
     * @param separator the line-separator for the client's platform.
     * @return An Output that produces good XML.
     */
    protected Output makeXMLOutput(PrintWriter out, String separator) {
        String filename = getRequestFileName();
        filename += ".xml";
        ResponseUtil.setXMLHeader(response, filename);
        return new StreamedOutput(out, new XMLFormatter(), separator);
    }

    /**
     * Make the default JSON output given the HttpResponse's PrintWriter.
     *
     * @param out The PrintWriter from the HttpResponse.
     * @param separator The line-separator for the client's platform.
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

    private PrintWriter out = null;

    private String lineBreak = null;

    /**
     * Get access to the underlying print-writer.
     *
     * Most services should not need this method.
     * @return The raw print-writer.
     */
    protected PrintWriter getRawOutput() {
        return out;
    }

    private void initOutput() {
        final String separator = getLineBreak();

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
            throw new ServiceException(e);
        }
        // TODO: retrieve the content types from the formats.
        String filename = getRequestFileName();
        switch (getFormat()) {
            case HTML:
                output = new StreamedOutput(out, new HTMLTableFormatter(),
                        separator);
                ResponseUtil.setHTMLContentType(response);
                break;
            case XML:
                output = makeXMLOutput(out, separator);
                break;
            case TSV:
                output = new StreamedOutput(out, new TabFormatter(
                        StringUtils.equals(getProperty("ws.tsv.quoted"), "true")),
                        separator);
                filename += ".tsv";
                if (isUncompressed()) {
                    ResponseUtil.setTabHeader(response, filename);
                }
                break;
            case CSV:
                output = new StreamedOutput(out, new CSVFormatter(), separator);
                filename += ".csv";
                if (isUncompressed()) {
                    ResponseUtil.setCSVHeader(response, filename);
                }
                break;
            case TEXT:
                output = new StreamedOutput(out, new PlainFormatter(), separator);
                filename += getExtension();
                if (isUncompressed()) {
                    ResponseUtil.setPlainTextHeader(response, filename);
                }
                break;
            case JSON:
                output = makeJSONOutput(out, separator);
                filename += ".json";
                if (isUncompressed()) {
                    ResponseUtil.setJSONHeader(response, filename, formatIsJSONP());
                }
                break;
            case OBJECTS:
                output = new StreamedOutput(out, new JSONObjectFormatter(),
                        separator);
                filename += ".json";
                if (isUncompressed()) {
                    ResponseUtil.setJSONHeader(response, filename, formatIsJSONP());
                }
                break;
            case TABLE:
                output = new StreamedOutput(out, new JSONTableFormatter(),
                        separator);
                filename = "resulttable.json";
                if (isUncompressed()) {
                    ResponseUtil.setJSONHeader(response, filename, formatIsJSONP());
                }
                break;
            case ROWS:
                output = new StreamedOutput(out, new JSONRowFormatter(), separator);
                if (isUncompressed()) {
                    ResponseUtil.setJSONHeader(response, "result.json", formatIsJSONP());
                }
                break;
            default:
                output = getDefaultOutput(out, os, separator);
        }
        if (!isUncompressed()) {
            ResponseUtil.setGzippedHeader(response, filename + getExtension());
            if (isZip()) {
                try {
                    ((ZipOutputStream) os).putNextEntry(new ZipEntry(filename));
                } catch (IOException e) {
                    throw new ServiceException(e);
                }
            }
        }
    }

    /**
     * @return The line separator for the client's platform.
     */
    public String getLineBreak() {
        if (lineBreak == null && request != null) {
            if (RequestUtil.isWindowsClient(request)) {
                lineBreak = Exporter.WINDOWS_SEPARATOR;
            } else {
                lineBreak = Exporter.UNIX_SEPARATOR;
            }
        }
        return lineBreak;
    }

    /**
     * @return The default file name for this service. (default = "result.tsv")
     */
    protected String getDefaultFileName() {
        return "result";
    }

    /**
     * If the request has a <code>filename</code> parameter then use that
     * for the fileName, otherwise use the default fileName
     * @return the fileName to use for the exported file
     */
    protected String getRequestFileName() {
        String param = WebServiceRequestParser.FILENAME_PARAMETER;
        String fileName = request.getParameter(param);
        if (StringUtils.isBlank(fileName)) {
            return getDefaultFileName();
        } else {
            return fileName.trim();
        }
    }

    /**
     * Make the default output for this service.
     *
     * @param out The response's PrintWriter.
     * @param os The Response's output stream.
     * @param separator The client's line separator.
     * @return An Output. (default = new StreamedOutput(out, new TabFormatter()))
     */
    protected Output getDefaultOutput(PrintWriter out, OutputStream os, String separator) {
        output = new StreamedOutput(out, new TabFormatter(), separator);
        ResponseUtil.setTabHeader(response, getDefaultFileName());
        return output;
    }

    /**
     * Make the default output for this service.
     *
     * @param out The response's PrintWriter.
     * @param os The Response's output stream.
     * @return An Output. (default = new StreamedOutput(out, new TabFormatter()))
     */
    protected Output getDefaultOutput(PrintWriter out, OutputStream os) {
        return getDefaultOutput(out, os, getLineBreak());
    }

    /**
     * Returns true if the request wants column headers as well as result rows
     *
     * @return true if the request declares it wants column headers
     */
    public boolean wantsColumnHeaders() {
        String wantsCols = request
                .getParameter(WebServiceRequestParser.ADD_HEADER_PARAMETER);
                      // Assume none wanted if empty
        boolean no = (wantsCols == null || wantsCols.isEmpty()
                      // interpret standard falsy values as false
                || "0".equals(wantsCols) || "false".equalsIgnoreCase(wantsCols)
                      // but none is what we really expect.
                || "none".equalsIgnoreCase(wantsCols));
        // All other values, including "true", "True", 1, and foo-bar are yes
        return !no;
    }

    /**
     * Get an enum which represents the column header style (path, friendly, or
     * none)
     *
     * @return a column header style
     */
    public ColumnHeaderStyle getColumnHeaderStyle() {
        if (wantsColumnHeaders()) {
            String style = request
                    .getParameter(WebServiceRequestParser.ADD_HEADER_PARAMETER);
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
     * @return The default format constant for this service.
     */
    protected Format getDefaultFormat() {
        return Format.EMPTY;
    }


    private Format format = null;
    private Boolean isJsonP = null;

    /**
     * Returns required output format.
     *
     * Cannot be overridden.
     *
     * @return format
     */
    public final Format getFormat() {
        if (format == null) {
            List<Format> askedFor = WebServiceRequestParser.getAcceptableFormats(request);
            if (askedFor.isEmpty()) {
                format = getDefaultFormat();
            } else {
                for (Format acceptable: askedFor) {
                    if (Format.DEFAULT == acceptable) {
                        format = getDefaultFormat();
                        break;
                    }
                    // Serve the first acceptable format.
                    if (canServe(acceptable)) {
                        format = acceptable;
                        break;
                    }
                }
                // Nothing --> NotAcceptable
                if (format == null) {
                    throw new NotAcceptableException();
                }
                // But empty --> default
                if (format == Format.EMPTY) {
                    format = getDefaultFormat();
                }
            }
        }

        return format;
    }

    /**
     * For very picky services, you can just set it yourself, and say "s****w you requester".
     *
     * Use this with caution, and fall-back to getFormat(). Please.
     *
     * @param format The format you have decided this request really wants.
     */
    protected void setFormat(Format format) {
        this.format = format;
    }

    /**
     * Get the value of the callback parameter.
     *
     * @return The value, or null if this request type does not support this.
     */
    public String getCallback() {
        if (formatIsJSONP()) {
            return getOptionalParameter(
                    WebServiceRequestParser.CALLBACK_PARAMETER,
                    DEFAULT_CALLBACK);
        } else {
            return null;
        }
    }

    /**
     * Determine whether a callback was supplied to this request.
     *
     * @return Whether or not a callback was supplied.
     */
    public boolean hasCallback() {
        return getOptionalParameter(WebServiceRequestParser.CALLBACK_PARAMETER) != null;
    }

    /**
     * Runs service. This is abstract method, that must be defined in subclasses
     * and so performs something useful. Standard procedure is overwrite this
     * method in subclasses and let this method to be called from
     * WebService.doGet method that encapsulates logic common for all web
     * services else you can overwrite doGet method in your web service class
     * and manage all the things alone.
     *
     * @throws Exception
     *             if some error occurs
     */
    protected abstract void execute() throws Exception;

    /**
     * @return true if this request has been authenticated to a specific
     *         existing user.
     */
    public boolean isAuthenticated() {
        return getPermission().getProfile() != ANON_PROFILE;
    }

    /**
     * Check whether the format is acceptable.
     *
     * By default returns true. Services with a particular set of accepted
     * formats should override this and check.
     * @param format The format to check.
     * @return whether or not this format is acceptable.
     */
    protected boolean canServe(Format format) {
        return format == getDefaultFormat();
    }

}
