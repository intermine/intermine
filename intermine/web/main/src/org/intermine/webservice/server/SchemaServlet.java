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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.util.StringUtil;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.intermine.webservice.server.output.Output;

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
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        runService(request, response);
    }

    private void runService(HttpServletRequest request,
            HttpServletResponse response) {

        if (StringUtils.isEmpty(request.getPathInfo())) {
            serveSchemaList(request, response);
        } else {
            serveSpecificSchema(request, response);
        }
    }

    private void serveSpecificSchema(HttpServletRequest req, HttpServletResponse resp) {
        String fileName = StringUtil.trimSlashes(req.getPathInfo());
        Properties webProperties = InterMineContext.getWebProperties();
        Set<String> schemata = new HashSet<String>(
            Arrays.asList(webProperties.getProperty("schema.filenames", "").split(",")));
        if (!schemata.contains(fileName)) {
            resp.setStatus(Output.SC_NOT_FOUND);
            try {
                PrintWriter pw = resp.getWriter();
                pw.println(fileName + " is not in the list of schemata.");
                pw.flush();
            } catch (IOException e) {
                LOGGER.error("Could not write response", e);
            }
        } else {
            try {
                req.getSession().getServletContext()
                    .getRequestDispatcher("/webservice/" + fileName).forward(req, resp);
            } catch (ServletException e) {
                throw new InternalErrorException(e);
            } catch (IOException e) {
                LOGGER.error("Could not write response", e);
            }
        }
    }

    private void serveSchemaList(HttpServletRequest req, HttpServletResponse resp) {
        final InterMineAPI im = InterMineContext.getInterMineAPI();
        new SchemaListService(im).service(req, resp);
    }

}
