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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import org.apache.commons.lang.StringUtils;
import org.intermine.web.context.InterMineContext;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/** @author Alex Kalderimis **/
public class ServiceListingHandler extends DefaultHandler
{

    private final Map<String, Object> result = new HashMap<String, Object>();
    private final List<Map<String, Object>> auth = new ArrayList<Map<String, Object>>();
    private final List<Map<String, Object>> endpoints
        = new ArrayList<Map<String, Object>>();

    private Map<String, Object> currentEndPoint = null;
    private List<Map<String, Object>> methods = null;
    private Map<String, Object> currentMethod = null;
    private List<Map<String, Object>> params = null;
    private Map<String, Object> currentParam = null;
    private List<Map<String, Object>> returns = null;
    private Map<String, Object> format = null;
    private List<Map<String, Object>> bodyFormats = null;
    private String bodyDescription = null;
    private Map<String, Object> currentContentType = null;
    private Stack<String> path = new Stack<String>();
    private StringBuffer sb = null;
    private JSONObject services = null;
    private Properties webProperties = InterMineContext.getWebProperties();
    private static final String DEFAULT_PROP_FMT = "ws.listing.default.%s.%s";

    @Override
    public void startDocument() {
        result.put("auth", auth);
        final Map<String, Object> tokenAuth = new HashMap<String, Object>();
        tokenAuth.put("name", "API key - parameter");
        tokenAuth.put("type", "token");
        tokenAuth.put("mechanism", "parameter");
        tokenAuth.put("key", "token");
        final Map<String, Object> tokenHeader = new HashMap<String, Object>();
        tokenHeader.put("name", "API key - header");
        tokenHeader.put("type", "token");
        tokenHeader.put("mechanism", "header");
        tokenHeader.put("key", "Authorization");
        tokenHeader.put("prefix", "Token ");
        final Map<String, Object> basicAuth = new HashMap<String, Object>();
        basicAuth.put("name", "Username and Password");
        basicAuth.put("type", "password");
        basicAuth.put("mechanism", "basic"); // Special shortcut definiton.
        auth.add(tokenAuth);
        auth.add(tokenHeader);
        auth.add(basicAuth);
        result.put("endpoints", endpoints);
    }

    @Override
    public void endDocument() {
        services = new JSONObject(result);
    }

    /** @return the fully parsed result **/
    public JSONObject getServices() {
        return services;
    }

    private void addToCurrentMethod(String key, Object value) throws SAXException {
        if (currentMethod == null) {
            throw new SAXException("Illegal document structure");
        }
        currentMethod.put(key, value);
    }

    private String deIndent(String input) {
        if (input == null) {
            return null;
        }
        input = input.replaceAll("^\\n", "").replaceAll("\\t", "    ");

        if (!input.startsWith(" ")) {
            return input;
        }

        int i = 1;
        while (input.charAt(i) == ' ') {
            i++;
        }
        String regex = "(?m)^\\s{" + i + "}";
        return input.replaceAll(regex, "");
    }

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attrs) throws SAXException {
        path.push(qName);
        if ("servlet-mapping".equals(qName)) {
            currentEndPoint = new HashMap<String, Object>();
            methods = new ArrayList<Map<String, Object>>();
            currentEndPoint.put("methods", methods);
        } else if ("returns".equals(qName)) {
            returns = new ArrayList<Map<String, Object>>();
            addToCurrentMethod("returnFormats", returns);
        } else if ("description".equals(qName)
                && "method".equals(path.get(Math.max(0, path.size() - 2)))) {
            currentMethod.put("DescriptionFormat", attrs.getValue("format"));
        } else if ("method".equals(qName)) {
            currentMethod = new HashMap<String, Object>();
            addToCurrentMethod("URI",
                String.valueOf(currentEndPoint.get("URI")).replaceAll("^/service", ""));
            addToCurrentMethod("HTTPMethod",
                    attrs.getValue("type"));
            addToCurrentMethod("RequiresAuthentication",
                    attrs.getValue("authenticationRequired"));
            final String slug = attrs.getValue("slug");
            if (slug != null) {
                addToCurrentMethod("URI", currentMethod.get("URI") + slug);
            }
            final String also = attrs.getValue("ALSO");
            if (also != null) {
                addToCurrentMethod("ALSO", also);
            }
            params = new ArrayList<Map<String, Object>>();
            currentMethod.put("parameters", params);
            methods.add(currentMethod);
        } else if ("body".equals(qName)) {
            bodyDescription = attrs.getValue("description");
            bodyFormats = new LinkedList<Map<String, Object>>();
        } else if ("content".equals(qName)) {
            currentContentType = new HashMap<String, Object>();
            currentContentType.put("contentType", attrs.getValue("type"));
            currentContentType.put("schema", attrs.getValue("schema"));
            bodyFormats.add(currentContentType);
        } else if ("param".equals(qName)) {
            currentParam = new HashMap<String, Object>();
            currentParam.put("Required",
                    Boolean.valueOf(attrs.getValue("required")) ? "Y" : "N");
            currentParam.put("Type", attrs.getValue("type"));
            currentParam.put("Description", attrs.getValue("description"));
            currentParam.put("Repeat", attrs.getValue("repeat"));
            currentParam.put("Depends", attrs.getValue("depends"));
            currentParam.put("Schema", attrs.getValue("schema"));
            currentParam.put("Options", attrs.getValue("options"));
            currentParam.put("Recommended", "true".equals(attrs.getValue("recommended")));
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

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        final String[] lines = sb.toString().replace("\n", "")
                                      .trim()
                                      .split("\n");
        StringBuffer contentBuffer = new StringBuffer();
        for (String line: lines) {
            contentBuffer.append(line.replaceAll("^ +", "") + "\n");
        }
        final String content = contentBuffer.toString().trim();

        if ("servlet-mapping".equals(qName)) {
            currentEndPoint = null;
        } else if ("url-pattern".equals(qName)) {
            if (currentEndPoint != null) {
                currentEndPoint.put("URI", StringUtils.chomp(content, "/*"));
            }
        } else if ("content".equals(qName)) {
            if (currentContentType != null) {
                // Don't trim and reflow text for bodies, but do de-indent it.
                currentContentType.put("example", deIndent(sb.toString()));
            }
            currentContentType = null;
        } else if ("body".equals(qName)) {
            if (bodyFormats != null && !bodyFormats.isEmpty()) {
                currentMethod.put("body", bodyFormats);
                currentMethod.put("bodyDescription", bodyDescription);
            }
            bodyFormats = null;
            bodyDescription = null;
        } else if ("servlet-name".equals(qName)) {
            if (content.startsWith("ws-") && currentEndPoint != null) {
                endpoints.add(currentEndPoint);
                currentEndPoint.put("name", content);
                currentEndPoint.put("identifier", content);
            }
        } else if ("name".equals(qName)) {
            if (path.contains("method")) {
                currentMethod.put("MethodName", content);
            } else if (path.contains("metadata")) {
                currentEndPoint.put("name", content);
            }
        } else if ("param".equals(qName)) {
            currentParam.put("Name", content);
            if (!currentParam.containsKey("Default")) {
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
            currentMethod.put("Description", deIndent(sb.toString()));
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
                // TODO: interpret accept info into header options here.
                for (Map<String, Object> map: returns) {
                    formatValues.add(String.valueOf(map.get("Name")));
                }
                formatParam.put("EnumeratedList", formatValues);
                params.add(formatParam);
            }
            returns = null;
        } else if ("method".equals(qName)) {
            if (currentMethod.containsKey("ALSO")) {
                String[] otherMethods
                    = String.valueOf(currentMethod.get("ALSO")).split(",");
                for (String m: otherMethods) {
                    Map<String, Object> aliasMethod
                        = new HashMap<String, Object>(currentMethod);
                    aliasMethod.put("HTTPMethod", m);
                    methods.add(aliasMethod);
                }
            }
            currentMethod = null;
        }

        path.pop();
    }

    @Override
    public void characters(char[] ch, int start, int length)
        throws SAXException {
        sb.append(ch, start, length);
    }
}
