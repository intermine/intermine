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
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Returns version of the deployed InterMine application.
 *
 * @author Jakub Kulaviak
 */
public class VersionServlet extends HttpServlet
{

    private static final Logger LOGGER = Logger.getLogger(VersionServlet.class);

    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}}
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        runService(request, response);
    }

    /**
     * {@inheritDoc}}
     */
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        runService(req, resp);
    }

    private void runService(HttpServletRequest request,
            HttpServletResponse response) {
        String pathFromUrl = request.getPathInfo();
        String version = getVersion(pathFromUrl, request);
        try {
            response.getWriter().print(version);
        } catch (IOException e) {
            LOGGER.error("Obtaining writer to write intermine version failed.", e);
        }
    }

    private String getVersion(String versionType, HttpServletRequest request) {
        if (versionType != null) {
            versionType = StringUtil.trimSlashes(versionType);

            if (versionType.equalsIgnoreCase("release")) {
                Properties webProperties =
                    SessionMethods.getWebProperties(request.getSession().getServletContext());
                return webProperties.getProperty("project.releaseVersion");
            } else if (versionType.equalsIgnoreCase("ws")) {
                return "" + Constants.WEB_SERVICE_VERSION;
            }
        }
        // for backwards compatibility default is the web service version
        return "" + Constants.WEB_SERVICE_VERSION;
    }
}
