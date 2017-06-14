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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

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
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.output.Output;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

/**
  * Provide a list of all the services available at this mine.
  * @author Alexis Kalderimis
  */
public class ServicesListingsServlet extends HttpServlet
{

    private static final Logger LOGGER = Logger.getLogger(VersionServlet.class);
    private static final String FILENAME = "services.json";
    private static final String WEB_XML = "web.xml";
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
        String path = getServletContext().getRealPath("/WEB-INF/" + WEB_XML);
        File webxml = new File(path);
        SAXParser parser = getParser();
        ServiceListingHandler handler = getHandler();
        try {
            parser.parse(webxml, handler);
            services = handler.getServices();
        } catch (IOException e) {
            throw new ServiceException(e);
        } catch (SAXException e) {
            throw new ServiceException(e);
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
            throw new ServiceException("Could not create a SAX parser");
        }
        return parser;
    }

    private ServiceListingHandler getHandler() {
        return new ServiceListingHandler();
    }

    private void setHeaders(String format, HttpServletRequest request,
            HttpServletResponse response) {
        if ("text".equalsIgnoreCase(format)) {
            ResponseUtil.setPlainTextHeader(response, FILENAME);
        } else {
            if (!StringUtils.isEmpty(request.getParameter("callback"))) {
                ResponseUtil.setJSONPHeader(response, FILENAME);
            } else {
                ResponseUtil.setJSONHeader(response, FILENAME);
            }
        }
    }

    private void runService(HttpServletRequest request,
            HttpServletResponse response) {
        String format = request.getParameter("format");
        String callback = request.getParameter("callback");
        setHeaders(format, request, response);

        if (services == null) {
            try {
                parseWebXML();
            } catch (ServiceException e) {
                response.setStatus(e.getHttpErrorCode());
                LOGGER.error(e);
                return;
            } catch (Throwable t) {
                response.setStatus(Output.SC_INTERNAL_SERVER_ERROR);
                LOGGER.error(t);
                t.printStackTrace();
                return;
            }
        }
        PrintWriter pw = null;
        try {
            response.setStatus(Output.SC_OK);
            pw = response.getWriter();

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
            pw.flush();
        } catch (IOException e) {
            response.setStatus(Output.SC_INTERNAL_SERVER_ERROR);
            LOGGER.error(e);
        } catch (Throwable e) {
            response.setStatus(Output.SC_INTERNAL_SERVER_ERROR);
            LOGGER.error("Unexpected error", e);
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
    }

}
