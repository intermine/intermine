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

import java.io.IOException;
import java.util.Properties;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.metadata.StringUtil;
import org.intermine.web.context.InterMineContext;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;

/**
  * Report whether a requested service is implementented on this
  * mine. This is intended to be used for non-core, primarily
  * biological services.
  *
  * @author Alexis Kalderimis
  */
public class AvailableServicesServlet extends HttpServlet
{
    private static final Logger LOGGER = Logger.getLogger(AvailableServicesServlet.class);

    private static final long serialVersionUID = 1L;

    private static final String NOT_SUPPORTED_MSG =
            "This webservice does not support this resource";

    /**
     * {@inheritDoc}}
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        runService(request, response);
    }

    /**
     * {@inheritDoc}}
     */
    public void doPost(HttpServletRequest req, HttpServletResponse resp) {
        runService(req, resp);
    }

    /**
     * {@inheritDoc}}
     */
    public void doPut(HttpServletRequest req, HttpServletResponse resp) {
        runService(req, resp);
    }

    private void runService(HttpServletRequest request, HttpServletResponse response) {
        String pathFromUrl = request.getPathInfo();

        try {
            String resourcePath = getResourcePath(pathFromUrl, request);
            response.getWriter().print(resourcePath);
        } catch (IOException e) {
            LOGGER.error(e);
        } finally {
            request.getSession().invalidate();
        }
    }

    // TODO - use xml config instead...
    private String getResourcePath(String resource, HttpServletRequest request) {
        if (resource != null) {
            resource = StringUtil.trimSlashes(resource);
            Properties webProperties = InterMineContext.getWebProperties();
            String resourcePath = webProperties.getProperty("resource.path." + resource);
            if (!StringUtils.isEmpty(resourcePath)) {
                return resourcePath;
            }
        }
        throw new ResourceNotFoundException(NOT_SUPPORTED_MSG + resource);
    }
}
