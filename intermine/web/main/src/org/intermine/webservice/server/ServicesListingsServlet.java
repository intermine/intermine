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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.webservice.server.output.Output;
import org.json.JSONException;
import org.json.JSONObject;

/**
  * Provide a list of all the services available at this mine.
  * @author Alexis Kalderimis
  */
public class ServicesListingsServlet extends HttpServlet
{

    private static final Logger LOGGER = Logger.getLogger(VersionServlet.class);
    private static final String FILENAME = "services.json";

    private static final long serialVersionUID = 1L;

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

    /**
     * {@inheritDoc}}
     */
    @Override
    public void doPut(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        runService(req, resp);
    }

    private void runService(HttpServletRequest request,
            HttpServletResponse response) {
        String format = request.getParameter("format");
        setHeaders(format, request, response);

        try {
            InputStream requestedFile = getClass().getResourceAsStream(FILENAME);
            PrintWriter pw = response.getWriter();
            String callback = request.getParameter("callback");
            if ("text".equalsIgnoreCase(format)) {
                try {
                    JSONObject jo = new JSONObject(IOUtils.toString(requestedFile));
                    for (String name: JSONObject.getNames(jo)) {
                        pw.println(jo.getJSONObject(name).getString("path"));
                    }
                } catch (JSONException e) {
                    response.setStatus(Output.SC_INTERNAL_SERVER_ERROR);
                    String message = "Error parsing JSON";
                    if (e.getMessage() != null) {
                        message = e.getMessage();
                    }
                    pw.println(message);
                }
            } else {
                if (!StringUtils.isEmpty(callback)) {
                    pw.write(callback + "(");
                }
                IOUtils.copy(requestedFile, pw);
                if (!StringUtils.isEmpty(callback)) {
                    pw.write(");");
                }
            }
        } catch (IOException e) {
            response.setStatus(Output.SC_INTERNAL_SERVER_ERROR);
            LOGGER.error(e);
        }
    }

    private void setHeaders(String format, HttpServletRequest request, HttpServletResponse response) {
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
}
