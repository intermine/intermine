package org.intermine.webservice.server.widget;

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
import java.io.StringReader;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parse webconfig-model.xml settings for a Report Widget and return it packaged up in JavaScript.
 * @author radek
 */
public class ReportWidgetsServlet extends HttpServlet
{

	private static final long serialVersionUID = 1L;

	// The arguments to replace.
	private static final String CALLBACK    = "#@+CALLBACK";
	private static final String TITLE       = "#@+TITLE";
	private static final String AUTHOR      = "#@+AUTHOR";
	private static final String DESCRIPTION = "#@+DESCRIPTION";
	private static final String VERSION     = "#@+VERSION";
	private static final String CONFIG      = "#@+CONFIG";
	
	// The config for all Widgets, serialized from `webconfig-model.xml`
	private static JSONObject widgetsWebConfig = null;
	private static String test = null;
	
    /**
     * {@inheritDoc}}
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        runService(request, response);
    }
    
	private void runService(HttpServletRequest request, HttpServletResponse response) {
    	// Do we have config?
        if (widgetsWebConfig == null) {
        	parseXML();
        }
    	
    	// Get request params.
        String paramId = request.getParameter("id");
        String paramCallback = request.getParameter("callback");
        
        // Set JavaScript header.
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/javascript");        
        
        // The response writer.
        PrintWriter pw = null;
		try {
			pw = response.getWriter();
		} catch (IOException e) { }
        
		if (pw != null) {
			pw.write(test);
		}
    }
    
	private void parseXML() {		
        String path = getServletContext().getRealPath("/WEB-INF/webconfig-model.xml");
        SAXParser parser = parser();
        try {
            parser.parse(new File(path), handler());
        } catch (IOException e) {
            throw new InternalErrorException(e);
        } catch (SAXException e) {
            throw new InternalErrorException(e);
        }
        
	}

    private SAXParser parser() {
        SAXParserFactory fac = SAXParserFactory.newInstance();
        SAXParser parser = null;
        try {
            parser = fac.newSAXParser();
        } catch (SAXException e) {
        	
        } catch (ParserConfigurationException e) { }
        if (parser == null) {
            throw new InternalErrorException("Could not create a SAX parser");
        }
        return parser;
    }
	
    private DefaultHandler handler() {
        DefaultHandler handler = new DefaultHandler() {
        	// Many widgets.
        	private JSONArray widgets = null;
        	// One widget.
        	private JSONObject widget = null;
        	// Many dependencies.
        	private JSONArray dependencies = null;
        	// Many PathQueries.
        	private JSONObject pathQueries = null;
        	// One PathQuery.
        	private String pathQueryName = null;
        	private String pathQuery = null;
        	// Other client config.
        	private JSONObject clientConfig = null;
        	
            public void startDocument() throws SAXException {
            	widgets = new JSONArray();
            }

            public void endDocument() throws SAXException {
            	test = widgets.toString();
            }

            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            	if ("reportwidget".equals(qName)) {
            		// Init new widget.
            		widget = new JSONObject();
            		// Save its direct attributes.
                    for (int i = 0; i < attributes.getLength(); i++) {
                        Object o = attributes.getValue(i);
                        // Make into Boolean value.
                        if ("true".equals(o) || "false".equals(o)) {
                            o = Boolean.valueOf(o.toString());
                        }
                        try {
							widget.put(attributes.getLocalName(i), o);
						} catch (JSONException e) { }
                    }
            	} else {
            		// If we have a current widget...
            		if (widget != null) {
            			try {
            				// Is it a PathQuery or its child?
            				if ("pathQuery".equals(qName) || pathQuery != null) {
    		                    if (pathQueries == null) pathQueries = new JSONObject();
    		                    
    		                    // Open?
    		                    if (pathQuery == null) {
    		                    	pathQuery = "<query";
    		                    } else {
    		                    	pathQuery += "<" + qName;
    		                    }
    		                    
    		                    // Query attributes.
    		                    for (int i = 0; i < attributes.getLength(); i++) {    		                    	
    		                    	String k = attributes.getLocalName(i);
    		                        String v = attributes.getValue(i);
    		                        
    		                    	// Check for PQ name.
    		                    	if ("pathQuery".equals(qName) && ("name".equals(k) || "title".equals("k"))) {
    		                    		pathQueryName = v;
    		                    	}
    		                        
    		                    	// Stringify back.
    		                        pathQuery += " " + k + "=\"" + v + "\"";
    		                    }
    		                    
    		                    // Close this level.
    		                    pathQuery += ">";
    		                    
            				} else {
                				// Parse the attrs into JSON.
    							JSONObject attrs = new JSONObject();
    		                    for (int i = 0; i < attributes.getLength(); i++) {
    		                        Object o = attributes.getValue(i);
    		                        // Make into Boolean value.
    		                        if ("true".equals(o) || "false".equals(o)) {
    		                            o = Boolean.valueOf(o.toString());
    		                        }
    		                        attrs.put(attributes.getLocalName(i), o);
    		                    }
    		                    
    		                    // Is it a dependency?
    		                    if ("dependency".equals(qName)) {
    		                    	if (dependencies == null) dependencies = new JSONArray();
    		                    	dependencies.put(attrs);
    		                    } else {
    		                    	// Do we have a PathQuery "open"?
    		                    	if (pathQuery != null) {
    		                    		pathQuery += qName + "/";
    		                    	} else {
    			                    	// A simple key value then.
    			                    	String key = attrs.getString("key");
    			                    	Object val = attrs.getString("value");
    			                    	if (key != null && val != null) {
    			                    		if (clientConfig == null) clientConfig = new JSONObject();
    			                    		clientConfig.put(key, val);
    			                    	} else {
    			                    		// Exception.
    			                    	}	
    		                    	}
    		                    }
            				}
						} catch (JSONException e) { }
            		}
            	}
            }

            public void endElement(String uri, String localName, String qName) throws SAXException {
            	// Closing one widget?
            	if ("reportwidget".equals(qName)) {
            		// Save the deps, pqs...
            		if (dependencies != null) {
            			try {
							widget.put("dependencies", dependencies);
						} catch (JSONException e) { }
            		}
            		if (pathQueries != null) {
            			// Save on client config.
            			if (clientConfig == null) clientConfig = new JSONObject();
            			try {
            				clientConfig.put("pathQueries", pathQueries);
						} catch (JSONException e) { }
            		}
            		if (clientConfig != null) {
            			try {
							widget.put("config", clientConfig);
						} catch (JSONException e) { }
            		}
            		
            		// Save the widget.
            		widgets.put(widget);

            		// Reset.
            		widget = null;
            		dependencies = null;
            		pathQueries = null;
            		clientConfig = null;
            	// Closing a PathQuery or its child?
            	} else if ("pathQuery".equals(qName)) {
            		// Do we have the PQ name?
            		if (pathQueryName == null) {
            			// Exception.
            		}
            		
            		// Close us up.
            		pathQuery += "</query>";
            		
            		// Convert to PathQuery.
            		PathQuery pq = PathQueryBinding.unmarshalPathQuery(new StringReader(pathQuery), PathQuery.USERPROFILE_VERSION);
            		if (!pq.isValid()) {
            			// Exception.
            		}
            		
            		// Save it.
            		try {
						pathQueries.put(pathQueryName, new JSONObject(pq.toJson()));
					} catch (JSONException e) { }
            		// Reset.
            		pathQuery = null;
            		pathQueryName = null;
            	} else if (pathQuery != null) {
            		// Close us up.
            		pathQuery += "</" + qName + ">";            		
            	}
            }

            public void characters(char ch[], int start, int length) throws SAXException {
            	
            }
        };
        return handler;
    }

}
