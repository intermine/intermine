package org.intermine.bio.webservice;

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
import org.intermine.api.InterMineAPI;
import org.intermine.web.logic.session.SessionMethods;

/**
 * A servlet to hand off to the UCSC BED query service.
 *
 * @author Fengyuan Hu
 *
 */
public class BEDQueryServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(BEDQueryServlet.class);

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

    private void runService(HttpServletRequest request,
            HttpServletResponse response) {
        // To avoid servlet caching always new service is created -->
        // Service has always new data and fields in executor are initialized
        // according new data
        // and not remember fields initialized according previous request data
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        new BEDQueryService(im).service(request, response);
    }
}
