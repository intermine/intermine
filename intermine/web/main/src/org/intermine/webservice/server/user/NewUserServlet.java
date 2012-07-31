package org.intermine.webservice.server.user;

/*
 * Copyright (C) 2002-2012 FlyMine
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
 * Servlet for handing off requests to the a NewUserService.
 * @author Alex Kalderimis.
 *
 */
public class NewUserServlet extends HttpServlet
{

    private static final long serialVersionUID = 2247791931782821682L;

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) {
        final InterMineAPI api = InterMineContext.getInterMineAPI();
        new NewUserService(api).service(req, resp);
    }
}
