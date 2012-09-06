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
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

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
	
	// The config, serialized from `webconfig-model.xml`
	private static JSONArray webConfigModel = null;
	
    /**
     * {@inheritDoc}}
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        runService(request, response);
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
    
    private void runService(HttpServletRequest request, HttpServletResponse response) {
    	// Do we have config?
        if (webConfigModel == null) {
        	webConfigModel = parseXML();
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
        
        // Find our config.
		if (pw != null) {
			JSONObject widget = getWidgetConfig(paramId);
			if (widget == null) {
				pw.write("new Error(\"Could not load config\")");
			} else {
				// Load the assoc file.
				String file = null;
				String path = getServletContext().getRealPath("/js/widgets/" + paramId + ".js");
				
				try {
					file = readInFile(path);
				} catch (IOException e) { }
				
				if (file == null) {
					file = "new Error(\"Could not load widget file\");";
				} else {
					// Remove the leading line preventing direct JS file use.
					String[] arr = file.split("\n");
					file = StringUtils.join(Arrays.copyOfRange(arr, 2, arr.length), "\n");
					
					// Make the simple param replacements.
					file = file.replaceAll(Pattern.quote(CALLBACK), paramCallback);
					try {
						file = file.replaceAll(Pattern.quote(TITLE), widget.getString("title"));
						file = file.replaceAll(Pattern.quote(AUTHOR), widget.getString("author"));
						file = file.replaceAll(Pattern.quote(DESCRIPTION), widget.getString("description"));
						file = file.replaceAll(Pattern.quote(VERSION), widget.getString("version"));
					} catch (JSONException e) { }
					
					// This is where we save the output config.
					JSONObject config = new JSONObject();
					
					// Parse keyValues JSONArray?
					JSONArray keyValues = getArray(widget, "keyValue");
					if (keyValues != null) {	
						for(int i = 0 ; i < keyValues.length() ; i++) {
							JSONObject o;
							try {
								o = keyValues.getJSONObject(i);
								config.put(o.getString("key"), o.getString("value"));
							} catch (JSONException e) { }
						}
					}
					
					// Add pathQueries JSONArray?
					JSONArray pathQueries = getArray(widget, "pathQuery");
					if (pathQueries != null) {
						try {
							config.put("pathQueries", pathQueries);
						} catch (JSONException e) { }
					}
					
					// Replace the config in the file with our config.
					file = file.replaceAll(Pattern.quote(CONFIG), config.toString());
				}
				
				// Write the output.
				pw.write(file);
			}
		}
    }

    private JSONArray getArray(JSONObject src, String key) {
    	JSONArray result = new JSONArray();
		try {
			// Is it a single object?
			JSONObject obj = src.getJSONObject(key);
			result.put(obj);
			return result;
		} catch (JSONException e0) {
			try {
				// An array maybe?
				return src.getJSONArray(key);
			} catch (JSONException e1) { }
		}
		return null;
    }
    
    private JSONObject getWidgetConfig(String id) {
    	if (webConfigModel == null) return null;
		
    	for(int i = 0 ; i < webConfigModel.length() ; i++) {
			try {
				JSONObject widget = webConfigModel.getJSONObject(i).getJSONObject("reportwidget");
				if (id.equals(widget.get("id"))) {
					return widget;
				}
			} catch (JSONException e) { }
		}
		return null;
    }
    
	private JSONArray parseXML() {		
		// Read in file.
        String path = getServletContext().getRealPath("/WEB-INF/webconfig-model.xml");
        String file = "";
        try {
			file = readInFile(path);
		} catch (IOException e) { }
        
		// XML to JSON.
        JSONObject json = new JSONObject();
        try {
        	json = XML.toJSONObject(file);
		} catch (JSONException e) {
			return null;
		}
		
		// Find the relevant section.
		JSONObject webconfig = new JSONObject();
		try {
			webconfig = json.getJSONObject("webconfig");
		} catch (JSONException e) {
			return null;
		}

		// Get only the Report Widgets.
		return getArray(webconfig, "reportwidgets");
	}

}
