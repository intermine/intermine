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

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.web.logic.export.ResponseUtil;

/**
 * Parse webconfig-model.xml settings for a Report Widget and return it packaged up in JavaScript.
 * @author radek
 */
public class ReportWidgetsServlet extends HttpServlet
{

	private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}}
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        runService(request, response);
    }
	
    private void runService(HttpServletRequest request, HttpServletResponse response) {
    	// Get request params.
        String id = request.getParameter("id");
        String callback = request.getParameter("callback");
        
        // Set JavaScript header.
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/javascript");

        // Write the file.
        try {
            PrintWriter pw = response.getWriter();
            pw.write("Hello world");
        } catch (IOException e) { }
    }

}
