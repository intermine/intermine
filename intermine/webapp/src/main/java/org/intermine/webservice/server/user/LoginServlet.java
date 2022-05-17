package org.intermine.webservice.server.user;

/*
 * Copyright (C) 2002-2022 FlyMine
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
 * Runs the login service
 * @author Daniela Butano
 *
 */
public class LoginServlet extends HttpServlet
{
    /**
     * {@inheritDoc}
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        runService(request, response);
    }

    private void runService(HttpServletRequest request, HttpServletResponse response) {
        final InterMineAPI im = InterMineContext.getInterMineAPI();
        new LoginService(im).service(request, response);
    }

}
