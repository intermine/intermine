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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang.StringUtils;
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
	private static JSONArray widgetsWebConfig = null;
	
    /**
     * {@inheritDoc}}
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        runService(request, response);
    }
    
	private void runService(HttpServletRequest request, HttpServletResponse response) {        
        // Set JavaScript header.
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/javascript");
        
        // The response writer.
        PrintWriter pw = null;
		try {
			pw = response.getWriter();
		} catch (IOException e) { }
        
		if (pw != null) {			
	    	// Do we have config?
	        if (widgetsWebConfig == null) {
	        	try {
					parseXML();
				} catch (ReportWidgetException e) {
					pw.write("new Error(\"Report Widget Config: " + e.getMessage() + "\");");
				}
	        }
	        
	    	// Do we have config?
	        if (widgetsWebConfig != null) {
	        	// Get request params.
	            String paramId = request.getParameter("id");
	            String paramCallback = request.getParameter("callback");	        	
	        	
	            if (paramId == null) {
	            	pw.write("new Error(\"Widget `id` not provided\");");
	            } else if (paramCallback == null) {
	            	pw.write("new Error(\"Widget `callback` not provided\");");
	            } else {
	            	// Find the relevant widget.
	            	JSONObject widget = null; 
	            	for (int i = 0; i < widgetsWebConfig.length(); i++) {
	            		try {
							JSONObject w = widgetsWebConfig.getJSONObject(i);
							if (w.get("id").equals(paramId)) {
								widget = w;
							}
						} catch (JSONException e) {
							pw.write("new Error(\"" + e.getMessage() + "\");");
						}
	            	}
	            	
	            	if (widget != null) {
	            		
	            		// Load the assoc file.
	    				String file = null;
	    				try {
	    					file = readInFile(getServletContext().getRealPath("/js/widgets/" + paramId + ".js"));
	    				} catch (IOException e) { }
	    				if (file != null) {
	    					// Remove the leading line preventing direct JS file use.
	    					String[] arr = file.split("\n");
	    					file = StringUtils.join(Arrays.copyOfRange(arr, 2, arr.length), "\n");
	    					
					        // Write the output.
					        pw.write(widgetsWebConfig.toString());
	    				} else {
	    					pw.write("new Error(\"Could not load widget file `/js/widgets/" + paramId + ".js`\");");
	    				}	            		
	            	} else {
	            		pw.write("new Error(\"Could not find widget `" + paramId + "`\");");
	            	}
	            }
	        }
		}
    }
    
    private String readInFile(String path) throws java.io.IOException {
        StringBuffer fileData = new StringBuffer(1000);
        BufferedReader reader = new BufferedReader(new FileReader(path));
        char[] buf = new char[1024];
        int numRead=0;
        while((numRead=reader.read(buf)) != -1){
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        return fileData.toString();
    }
	
	private void parseXML() throws ReportWidgetException {		
        String path = getServletContext().getRealPath("/WEB-INF/webconfig-model.xml");
        SAXParser parser = parser();
        try {
            parser.parse(new File(path), handler());
        } catch (IOException e) {
        	throw new ReportWidgetException(e.getMessage());
        } catch (SAXException e) {
            throw new ReportWidgetException(e.getMessage());
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
            	widgetsWebConfig = widgets;
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
						} catch (JSONException e) {
							throw new SAXException("Error saving attributes of `<query>`.");
						}
                    }
            	} else {
            		// If we have a current widget...
            		if (widget != null) {
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
		                        try {
									attrs.put(attributes.getLocalName(i), o);
								} catch (JSONException e) {
									throw new SAXException("Error saving attributes of `<" + qName + ">`.");
								}
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
			                    	String key;
									try {
										key = attrs.getString("key");
									} catch (JSONException e) {
										throw new SAXException("This key-value pair does not have the `key` attr.");
									}
			                    	Object val;
									try {
										val = attrs.getString("value");
									} catch (JSONException e) {
										throw new SAXException("This key-value pair does not have the `value` attr.");
									}
			                    	if (key != null && val != null) {
			                    		if (clientConfig == null) clientConfig = new JSONObject();
			                    		try {
											clientConfig.put(key, val);
										} catch (JSONException e) {
											throw new SAXException("Could not save client config key-value pair.");
										}
			                    	} else {
			                    		throw new SAXException("This key-value pair does not have the `key` and `attr` attrs defined.");
			                    	}	
		                    	}
		                    }
        				}
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
						} catch (JSONException e) {
							throw new SAXException("Error saving `dependencies` on widget.");
						}
            		}
            		if (pathQueries != null) {
            			// Save on client config.
            			if (clientConfig == null) clientConfig = new JSONObject();
            			try {
            				clientConfig.put("pathQueries", pathQueries);
						} catch (JSONException e) {
							throw new SAXException("Error saving `pathQuery` on widget.");
						}
            		}
            		if (clientConfig != null) {
            			try {
							widget.put("config", clientConfig);
						} catch (JSONException e) {
							throw new SAXException("Error saving `config` on widget.");
						}
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
            			throw new SAXException("PathQuery does not have a `name` or `title` attr defined. See, not I can't even tell you which PathQuery this is...");
            		}
            		
            		// Close us up.
            		pathQuery += "</query>";
            		
            		// Convert to PathQuery.
            		PathQuery pq = PathQueryBinding.unmarshalPathQuery(new StringReader(pathQuery), PathQuery.USERPROFILE_VERSION);
            		// Do not check the validity of this PQ, as we would check the Model too even though we may run this PQ against a different mine.
            		//if (!pq.isValid()) {
					//	throw new SAXException("PathQuery `" + pathQueryName + "` is invalid (" + StringUtils.join(pq.verifyQuery(), ", ") + ").");
            		//}
            		
            		// Save it.
            		try {
						pathQueries.put(pathQueryName, new JSONObject(pq.toJson()));
					} catch (JSONException e) {
						throw new SAXException("Error saving `" + pathQueryName + "` on `pathQueries`.");
					}
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

    class ReportWidgetException extends Exception {
    	
    	private static final long serialVersionUID = 1L;
		private String message;
    	
		public ReportWidgetException(String message) {
			super(message);
			this.message = message;
		}
		
		public String getError() {
			return message;
		}
    	
    }
    
}
