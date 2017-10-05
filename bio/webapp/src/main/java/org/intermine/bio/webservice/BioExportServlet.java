package org.intermine.bio.webservice;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.intermine.web.context.InterMineContext;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.core.NoServiceException;
import org.intermine.webservice.server.core.WebServiceServlet;

/**
 * A servlet to hand off to the GFF3-query-service.
 * @author Alex Kalderimis.
 *
 */
public abstract class BioExportServlet extends WebServiceServlet
{
    private static final long serialVersionUID = 1L;
    /**
     * so class names
     */
    public static final String SO_CLASS_NAMES = "SO_CLASS_NAMES";
    private static final String RESOURCE = "/WEB-INF/soClassName.properties";

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() throws ServletException {
        if (InterMineContext.getAttribute(SO_CLASS_NAMES) == null) {
            ServletContext sc = getServletContext();
            Properties soNameProperties = new Properties();
            try {
                InputStream is = sc.getResourceAsStream(RESOURCE);
                if (is == null) {
                    throw new ServletException("Could not find " + RESOURCE);
                }
                soNameProperties.load(is);
            } catch (Exception e) {
                throw new ServletException("Error loading so class name mapping file", e);
            }
            InterMineContext.setAttribute(SO_CLASS_NAMES, soNameProperties);
        }
    }

    /**
     * @return web service
     */
    protected abstract WebService getService();

    @Override
    protected WebService getService(Method method) throws NoServiceException {
        switch (method) {
            case GET:
                return getService();
            case POST:
                return getService();
            default:
                throw new NoServiceException();
        }
    }
}
