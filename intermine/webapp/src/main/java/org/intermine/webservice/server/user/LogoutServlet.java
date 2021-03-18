package org.intermine.webservice.server.user;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.api.InterMineAPI;
import org.intermine.web.context.InterMineContext;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Runs the logout service
 * @author Daniela Butano
 *
 */
public class LogoutServlet extends HttpServlet
{
    /**
     * {@inheritDoc}
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        runService(request, response);
    }

    private void runService(HttpServletRequest request, HttpServletResponse response) {
        final InterMineAPI im = InterMineContext.getInterMineAPI();
        new LogoutService(im).service(request, response);
    }

}
