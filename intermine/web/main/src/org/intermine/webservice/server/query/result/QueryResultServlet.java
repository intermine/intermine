package org.intermine.webservice.server.query.result;

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
import org.intermine.webservice.server.WebService;



/**
 * Servlet that runs QueryResultService.
 * @see org.intermine.webservice.server.query.result.QueryResultService for more information.
 * @author Jakub Kulaviak
 */
public class QueryResultServlet extends HttpServlet
{
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
        // To avoid servlet caching always new service is created -->
        // Service has always new data and fields in executor are initialized
        // according new data
        // and not remember fields initialized according previous request data
        String pathInfo = request.getPathInfo();
        final InterMineAPI im = InterMineContext.getInterMineAPI();
        WebService ws;
        if (pathInfo != null && pathInfo.endsWith("tablerows")) {
            ws = new TableRowService(im);
        } else {
            ws = new QueryResultService(im);
        }
        
        ws.service(request, response);
    }
}
