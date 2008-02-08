package org.intermine.webservice;

/*
 * Copyright (C) 2002-2007 FlyMine
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

/**
 * ServiceServlet executes InterMine web service. @see org.intermine.webservice.ServiceExecutor
 * for more information.
 * @author Jakub Kulaviak
 **/
public class ServiceServlet extends HttpServlet
{


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
            HttpServletResponse response) throws ServletException, IOException  {
        // To avoid servlet caching always new executor is created -->
        // Executor has always new data and fields in executor are initialized according new data
        // and not remember fields initialized according previous request data
        new ServiceExecutor().runService(request, response);
    }
}
