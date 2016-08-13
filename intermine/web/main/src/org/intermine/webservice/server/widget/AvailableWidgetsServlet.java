package org.intermine.webservice.server.widget;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.InterMineAPI;
import org.intermine.web.context.InterMineContext;

/**
 * Servlet handling requests for information about which widgets are
 * available.
 * @author Alex Kalderimis
 *
 */
public final class AvailableWidgetsServlet extends HttpServlet
{

    /**
     * Generated serial id.
     */
    private static final long serialVersionUID = 4536224836847168699L;

    /**
     * {@inheritDoc}}
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        runService(req, resp);
    }

    private void runService(HttpServletRequest request, HttpServletResponse response) {
        // To avoid servlet caching always new service is created -->
        // Service has always new data and fields in executor are initialized
        // according new data
        // and not remember fields initialized according previous request data
        final InterMineAPI im = InterMineContext.getInterMineAPI();
        new AvailableWidgetsService(im).service(request, response);
    }

}
