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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.exceptions.ResourceNotFoundException;

/**
 * Returns a requested schema.
 *
 * @author Alexis Kalderimis
 */
public class SchemaServlet extends HttpServlet
{

	private static final Logger LOGGER = Logger.getLogger(SchemaServlet.class);

	private static final long serialVersionUID = 1L;

	/**
	 * {@inheritDoc}}
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
			runService(request, response);
		}

	private void runService(HttpServletRequest request,
			HttpServletResponse response) {

        setHeaders(request, response);

		try {
			InputStream requestedFile = getFile(request);
            PrintWriter pw = response.getWriter();
            String callback = request.getParameter("callback");
            if (!StringUtils.isEmpty(callback)) {
                pw.write(callback + "(");
            }
			IOUtils.copy(requestedFile, pw);
            if (!StringUtils.isEmpty(callback)) {
                pw.write(");");
            }
		} catch (IOException e) {
			LOGGER.error("Obtaining writer to handle schema request failed.", e);
		} catch (ResourceNotFoundException e) {
			response.setStatus(e.getHttpErrorCode());
            try {
                response.getWriter().print(e.getMessage());
            } catch (IOException ex) {
                LOGGER.error("Obtaining writer to handle schema request failed.", ex);
            }
			return;
		}
	}

    private void setHeaders(HttpServletRequest request, HttpServletResponse response) {
		String fileName = StringUtil.trimSlashes(request.getPathInfo());
        if (fileName == null) {
            return;
        }

        if (fileName.endsWith("xsd")) {
            ResponseUtil.setXMLHeader(response, fileName);
        } else if (fileName.endsWith("schema")) {
            if (!StringUtils.isEmpty(request.getParameter("callback"))) {
                ResponseUtil.setJSONPHeader(response, fileName);
            } else {
                ResponseUtil.setJSONSchemaHeader(response, fileName);
            }
        }
    }


	private InputStream getFile(HttpServletRequest request) {
		String fileName = StringUtil.trimSlashes(request.getPathInfo());
		Properties webProperties =
			SessionMethods.getWebProperties(request.getSession().getServletContext());
		Set<String> schemata = new HashSet<String>(
			Arrays.asList(webProperties.getProperty("schema.filenames", "").split(",")));
		if (!schemata.contains(fileName)) {
			throw new ResourceNotFoundException(fileName + " is not in the list of schemata.");
		} else {
			InputStream is = getClass().getResourceAsStream(fileName);
			if (is == null) {
				throw new ResourceNotFoundException(fileName + 
					" is one of our schemata, but its corresponding file was not found.");
			} 
			return is;
		}
	}
}
