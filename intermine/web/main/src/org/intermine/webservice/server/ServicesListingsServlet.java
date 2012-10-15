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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.intermine.webservice.server.output.Output;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
  * Provide a list of all the services available at this mine.
  * @author Alexis Kalderimis
  */
public class ServicesListingsServlet extends HttpServlet
{

    private static final Logger LOGGER = Logger.getLogger(VersionServlet.class);
    //private static final String FILENAME = "services.json";
    private static final String FILENAME = "web.xml";
    private static final long serialVersionUID = 1L;

    private static JSONObject services = null;

    /**
     * {@inheritDoc}}
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        runService(request, response);
    }

    /**
     * {@inheritDoc}}
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        runService(req, resp);
    }

    private void parseWebXML() {
        String path = getServletContext().getRealPath("/WEB-INF/" + FILENAME);
        File webxml = new File(path);
        SAXParser parser = getParser();
        DefaultHandler handler = getHandler();
        try {
            parser.parse(webxml, handler);
        } catch (IOException e) {
            throw new InternalErrorException(e);
        } catch (SAXException e) {
            throw new InternalErrorException(e);
        }
    }

    private SAXParser getParser() {
        SAXParserFactory fac = SAXParserFactory.newInstance();
        SAXParser parser = null;
        try {
            parser = fac.newSAXParser();
        } catch (SAXException e) {
            LOGGER.error(e);
        } catch (ParserConfigurationException e) {
            LOGGER.error(e);
        }
        if (parser == null) {
            throw new InternalErrorException("Could not create a SAX parser");
        }
        return parser;
    }

    private DefaultHandler getHandler() {
        DefaultHandler handler = new DefaultHandler() {
            private final Map<String, Object> result = new HashMap<String, Object>();
            private final List<Map<String, Object>> endpoints
                = new ArrayList<Map<String, Object>>();
            private Map<String, Object> currentEndPoint = null;
            private List<Map<String, Object>> methods = null;
            private Map<String, Object> currentMethod = null;
            private List<Map<String, Object>> params = null;
            private Map<String, Object> currentParam = null;
            private List<Map<String, Object>> returns = null;
            private Map<String, Object> format = null;
            private Stack<String> path = new Stack<String>();
            private StringBuffer sb = null;
            private Properties webProperties = InterMineContext.getWebProperties();
            private static final String DEFAULT_PROP_FMT = "ws.listing.default.%s.%s";

            public void startDocument() {
                result.put("endpoints", endpoints);
            }

            public void endDocument() {
                services = new JSONObject(result);
            }

            public void startElement(String uri, String localName, String qName,
                    Attributes attrs) throws SAXException {
                path.push(qName);
                if ("servlet-mapping".equals(qName)) {
                    currentEndPoint = new HashMap<String, Object>();
                    methods = new ArrayList<Map<String, Object>>();
                    currentEndPoint.put("methods", methods);
                } else if ("returns".equals(qName)) {
                    returns = new ArrayList<Map<String, Object>>();
                    currentMethod.put("returnFormats", returns);
                } else if ("method".equals(qName)) {
                    currentMethod = new HashMap<String, Object>();
                    currentMethod.put("URI",
                        String.valueOf(currentEndPoint.get("URI")).replaceAll("^/service", ""));
                    currentMethod.put("HTTPMethod",
                            attrs.getValue("type"));
                    currentMethod.put("RequiresAuthentication",
                            attrs.getValue("authenticationRequired"));
                    if (attrs.getValue("slug") != null) {
                    	currentMethod.put("URI",
                    		currentMethod.get("URI") + attrs.getValue("slug"));
                    }
                    String also = attrs.getValue("ALSO");
                    if (also != null) {
                        currentMethod.put("ALSO", also);
                    }
                    params = new ArrayList<Map<String, Object>>();
                    currentMethod.put("parameters", params);
                    methods.add(currentMethod);
                } else if ("param".equals(qName)) {
                    currentParam = new HashMap<String, Object>();
                    currentParam.put("Required",
                            Boolean.valueOf(attrs.getValue("required")) ? "Y" : "N");
                    currentParam.put("Type", attrs.getValue("type"));
                    currentParam.put("Description", attrs.getValue("description"));
                    String defaultValue = attrs.getValue("default");
                    if (defaultValue != null) {
                        currentParam.put("Default", defaultValue);
                    }
                    
                    if ("enumerated".equals(currentParam.get("Type"))) {
                        currentParam.put("EnumeratedList",
                                Arrays.asList(attrs.getValue("values").split(",")));
                    }
                    params.add(currentParam);
                } else if ("format".equals(qName)) {
                    format = new HashMap<String, Object>();
                    int attrLen = attrs.getLength();
                    for (int i = 0; i < attrLen; i++) {
                        String ln = attrs.getLocalName(i);
                        format.put(ln, attrs.getValue(i));
                    }
                    returns.add(format);
                }
                sb = new StringBuffer();
            }

            public void endElement(String uri, String localName,
                    String qName) throws SAXException {

                final String content = sb.toString().replace("\n", "")
                                              .trim()
                                              .replaceAll(" +", " ");

                if ("servlet-mapping".equals(qName)) {
                    currentEndPoint = null; 
                } else if ("url-pattern".equals(qName)) {
                    if (currentEndPoint != null) {
                        currentEndPoint.put("URI", StringUtils.chomp(content, "/*"));
                    }
                } else if ("servlet-name".equals(qName)) {
                    if (content.startsWith("ws-") && currentEndPoint != null) {
                        endpoints.add(currentEndPoint);
                        currentEndPoint.put("name", content);
                    }
                } else if ("name".equals(qName)) {
                    if (path.contains("method")) {
                        currentMethod.put("MethodName", content);
                    } else if (path.contains("metadata")) {
                        currentEndPoint.put("name", content);
                    }
                } else if ("param".equals(qName)) {
                    currentParam.put("Name", content);
                    if (!currentParam.containsKey("Default")){
                        // Resolve configured default, if possible and not already set.
                        String configuredDefault = webProperties.getProperty(
                            String.format(DEFAULT_PROP_FMT, currentEndPoint.get("URI"), content));
                        if (configuredDefault != null) {
                            currentParam.put("Default", configuredDefault);
                        }
                    }
                    currentParam = null;
                } else if ("format".equals(qName)) {
                    format.put("Name", content);
                    format = null;
                } else if ("summary".equals(qName)) {
                    currentMethod.put("Synopsis", content);
                } else if ("description".equals(qName)) {
                    currentMethod.put("Description", content);
                } else if ("minVersion".equals(qName)) {
                    Integer minVersion = Integer.valueOf(content);
                    currentEndPoint.put("minVersion", minVersion);
                } else if ("returns".equals(qName)) {
                    if (returns.size() > 1) {
                        Map<String, Object> formatParam = new HashMap<String, Object>();
                        formatParam.put("Name", "format");
                        formatParam.put("Required", "N");
                        formatParam.put("Default", returns.get(0).get("Name"));
                        formatParam.put("Type", "enumerated");
                        formatParam.put("Description", "Output format");
                        List<String> formatValues = new ArrayList<String>();
                        for (Map<String, Object> obj: returns) {
                            formatValues.add(String.valueOf(obj.get("Name")));
                        }
                        formatParam.put("EnumeratedList", formatValues);
                        params.add(formatParam);
                    }
                    returns = null;
                } else if ("method".equals(qName)) {
                    if (currentMethod.containsKey("ALSO")) {
                        String[] otherMethods = String.valueOf(currentMethod.get("ALSO")).split(",");
                        for (String m: otherMethods) {
                            Map<String, Object> aliasMethod = new HashMap<String, Object>(currentMethod);
                            aliasMethod.put("HTTPMethod", m);
                            methods.add(aliasMethod);
                        }
                    }
                    currentMethod = null;
                }

                path.pop();
            }

            public void characters(char[] ch, int start, int length)
                throws SAXException {
                sb.append(ch, start, length);
            }
        };
        return handler;
    }

    private void setHeaders(String format, HttpServletRequest request,
            HttpServletResponse response) {
        if ("text".equalsIgnoreCase(format)) {
            ResponseUtil.setPlainTextHeader(response, FILENAME);
        } else {
            if (!StringUtils.isEmpty(request.getParameter("callback"))) {
                ResponseUtil.setJSONPHeader(response, FILENAME);
            } else {
                ResponseUtil.setJSONSchemaHeader(response, FILENAME);
            }
        }
    }

    private void runService(HttpServletRequest request,
            HttpServletResponse response) {
        String format = request.getParameter("format");
        String callback = request.getParameter("callback");
        setHeaders(format, request, response);

        if (services == null) {
            parseWebXML();
        }
        try {
            PrintWriter pw = response.getWriter();

            if ("text".equalsIgnoreCase(format)) {
                try {
                    for (String name: JSONObject.getNames(services)) {
                        pw.println(services.getJSONObject(name).getString("path"));
                    }
                } catch (JSONException e) {
                    response.setStatus(Output.SC_INTERNAL_SERVER_ERROR);
                    String message = "Error parsing services";
                    if (e.getMessage() != null) {
                        message = e.getMessage();
                    }
                    pw.print("[ERROR] ");
                    pw.println(message);
                }
            } else {
                if (!StringUtils.isEmpty(callback)) {
                    pw.write(callback + "(");
                }
                pw.write(services.toString());
                if (!StringUtils.isEmpty(callback)) {
                    pw.write(");");
                }
            }
        } catch (IOException e) {
            response.setStatus(Output.SC_INTERNAL_SERVER_ERROR);
            LOGGER.error(e);
        }
    }

}
