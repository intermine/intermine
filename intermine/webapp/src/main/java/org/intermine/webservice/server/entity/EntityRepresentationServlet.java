package org.intermine.webservice.server.entity;

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
 *
 * @author Daniela Butano
 */
public class EntityRepresentationServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}}
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        runService(request, response);
    }

    /**
     * {@inheritDoc}}
     */
    public void doPost(HttpServletRequest req, HttpServletResponse resp) {
        runService(req, resp);
    }

    private void runService(HttpServletRequest request, HttpServletResponse response) {
        final InterMineAPI api = InterMineContext.getInterMineAPI();
        new EntityRepresentationService(api).service(request, response);
    }
}
