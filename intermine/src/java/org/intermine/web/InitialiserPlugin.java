package org.flymine.web;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletContext;

import org.apache.struts.action.ActionServlet;
import org.apache.struts.action.PlugIn;
import org.apache.struts.config.ModuleConfig;

import org.flymine.web.config.WebConfig;


/**
 * Initialiser for the FlyMine web application
 * Anything that needs global initialisation goes here.
 *
 * @author Andrew Varley
 */

public class InitialiserPlugin implements PlugIn
{

    /**
     * Init method called at Servlet initialisation
     *
     * @param servlet ActionServlet that is managing all the modules
     * in this web application
     * @param config ModuleConfig for the module with which this
     * plug-in is associated
     *
     * @throws ServletException if this <code>PlugIn</code> cannot
     * be successfully initialized
     */
    public void init(ActionServlet servlet, ModuleConfig config)
        throws ServletException {

        ServletContext context = servlet.getServletContext();

        try {
            InputStream is = context.getResourceAsStream("/WEB-INF/webconfig-model.xml");
            WebConfig wc = WebConfig.parse(is);
            context.setAttribute("webconfig", wc);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     * Destroy method called at Servlet destroy
     */
    public void destroy() {
    }

}
