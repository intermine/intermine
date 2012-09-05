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
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Parse webconfig-model.xml settings for a Report Widget and return it packaged up in JavaScript.
 * @author radek
 */
public class ReportWidgetsServlet extends HttpServlet
{

	private static final long serialVersionUID = 1L;

	// The arguments to replace.
	private static final String CALLBACK = "#@+CALLBACK";
	
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
    	// Get request params.
        String paramId = request.getParameter("id");
        String paramCallback = request.getParameter("callback");
        
        // Set JavaScript header.
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/javascript");

        String file = "new Error(\"Could not load widget file\");";
        String path = getServletContext().getRealPath("/js/widgets/publications-displayer.js");
        try {
            // Read in the precompiled file.
			file = readInFile(path);
			// Replace the callback.
			file = file.replaceAll(Pattern.quote(CALLBACK), paramCallback);
		} catch (IOException e) { }
        
        // Write the file.
        try {
            PrintWriter pw = response.getWriter();
            pw.write(file);
        } catch (IOException e) { }
    }

}
