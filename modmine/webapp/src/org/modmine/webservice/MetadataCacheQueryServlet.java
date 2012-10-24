package org.modmine.webservice;

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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * A servlet to hand off to the modMine MetadataCache query service.
 *
 * @author Fengyuan Hu
 *
 */
public class MetadataCacheQueryServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static final Logger LOG = Logger
            .getLogger(MetadataCacheQueryServlet.class);

    private static final String URL_PATTERN = "/service/query/metadatacache";
    private static final int URL_PATTERN_LENGTH = URL_PATTERN.length(); // length = 28

    /**
     * {@inheritDoc}
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        try {
            runService(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        try {
            runService(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runService(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        // To avoid servlet caching always new service is created -->
        // Service has always new data and fields in executor are initialized
        // according new data
        // and not remember fields initialized according previous request data

        String resourcePath;
        String requestURI = request.getRequestURI();

        int start = requestURI.indexOf(URL_PATTERN) + URL_PATTERN_LENGTH;
        if (start == requestURI.length()) {
            resourcePath = "metadatacache";
        } else {
            String resourceURI = requestURI.substring(start);

            if ("/".equals(resourceURI)) {
                resourcePath = "metadatacache";
            } else {
                resourceURI = resourceURI.startsWith("/") ? resourceURI.substring(1) : resourceURI;
                resourceURI = resourceURI.endsWith("/") ? resourceURI.substring(0,
                        resourceURI.length() - 1) : resourceURI;
                resourcePath = resourceURI.replaceAll("/", ".");
            }
        }
        new MetadataCacheQueryService().service(request, response, resourcePath);
    }
}
