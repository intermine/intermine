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
            private final List<Map<String, Object>> resources
                = new ArrayList<Map<String, Object>>();
            private Map<String, Object> currentService = null;
            private Map<String, Object> methods = null;
            private Map<String, Object> method = null;
            private Map<String, Object> params = null;
            private Map<String, Object> param = null;
            private Map<String, Object> returns = null;
            private Map<String, Object> format = null;
            private Stack<String> path = new Stack<String>();
            private StringBuffer sb = null;

            public void startDocument() {
                result.put("resources", resources);
            }

            public void endDocument() {
                services = new JSONObject(result);
            }

            public void startElement(String uri, String localName, String qName,
                    Attributes attrs) throws SAXException {
                path.push(qName);
                if ("servlet-mapping".equals(qName)) {
                    currentService = new HashMap<String, Object>();
                } else if ("params".equals(qName)) {
                    params = new HashMap<String, Object>();
                    currentService.put("parameters", params);
                } else if ("methods".equals(qName)) {
                    methods = new HashMap<String, Object>();
                    currentService.put("methods", methods);
                } else if ("returns".equals(qName)) {
                    returns = new HashMap<String, Object>();
                    currentService.put("returnFormats", returns);
                } else if ("method".equals(qName)) {
                    method = new HashMap<String, Object>();
                    int attrLen = attrs.getLength();
                    for (int i = 0; i < attrLen; i++) {
                        String ln = attrs.getLocalName(i);
                        Object o = attrs.getValue(i);
                        if ("true".equals(o) || "false".equals(o)) {
                            o = Boolean.valueOf(o.toString());
                        }
                        method.put(ln, o);
                    }
                } else if ("param".equals(qName)) {
                    param = new HashMap<String, Object>();
                    param.put("required",
                            Boolean.valueOf(attrs.getValue("required")));
                    param.put("type", attrs.getValue("type"));
                    param.put("description", attrs.getValue("description"));
                } else if ("format".equals(qName)) {
                    format = new HashMap<String, Object>();
                    int attrLen = attrs.getLength();
                    for (int i = 0; i < attrLen; i++) {
                        String ln = attrs.getLocalName(i);
                        format.put(ln, attrs.getValue(i));
                    }
                }
                sb = new StringBuffer();
            }
            public void endElement(String uri, String localName,
                    String qName) throws SAXException {
                Set<String> properties = new HashSet<String>(Arrays.asList(
                            "name", "description"));
                String content = sb.toString().replace("\n", "")
                                              .trim()
                                              .replaceAll(" +", " ");
                if ("servlet-mapping".equals(qName)) {
                    currentService = null;
                } else if ("url-pattern".equals(qName)) {
                    if (currentService != null) {
                        currentService.put("path", StringUtils.chomp(content, "/*"));
                    }
                } else if ("servlet-name".equals(qName)) {
                    if (sb.toString().startsWith("ws-") && currentService != null) {
                        resources.add(currentService);
                    }
                } else if (properties.contains(qName)) {
                    currentService.put(qName, content);
                } else if ("method".equals(qName)) {
                    methods.put(content, method);
                } else if ("param".equals(qName)) {
                    params.put(content, param);
                } else if ("format".equals(qName)) {
                    returns.put(content, format);
                } else if ("authentication-required".equals(qName)) {
                    Boolean mustAuthenticate = Boolean.valueOf(content);
                    currentService.put("authenticationRequired", mustAuthenticate);
                } else if ("minVersion".equals(qName)) {
                    Integer minVersion = Integer.valueOf(content);
                    currentService.put("minVersion", minVersion);
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
