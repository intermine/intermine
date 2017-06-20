package org.intermine.webservice.server;

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

import org.intermine.web.context.InterMineContext;

/**
 * A Servlet for routing requests to the ClassKeyService.
 *
 * @author Alexis Kalderimis
 */
public class ClassKeyServlet extends HttpServlet
{

    private static final long serialVersionUID = -8916814874009422133L;

    /**
     * {@inheritDoc}}
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        WebService s = new ClassKeysService(InterMineContext.getInterMineAPI());
        s.service(request, response);
    }

}
