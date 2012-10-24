package org.intermine.webservice.server.path;

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

import org.intermine.api.InterMineAPI;
import org.intermine.web.context.InterMineContext;

/**
 * A servlet for routing requests to the possible values service.
 * @author ajk59
 *
 */
public class PossibleValuesServlet extends HttpServlet
{

    /**
     * Generated serial ID.
     */
    private static final long serialVersionUID = 8496670709870861844L;

    /**
     *  Constructor.
     */
    public PossibleValuesServlet() {
        // Empty constructor.
    }

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
        final InterMineAPI im = InterMineContext.getInterMineAPI();
        new PossibleValuesService(im).service(request, response);
    }
}
