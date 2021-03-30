package org.intermine.bio.webservice;

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
 * Runs ExternalLinksService web service.
 *
 * @author sc
 */
public class ExternalLinksServlet extends HttpServlet
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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        runService(req, resp);
    }


    private void runService(HttpServletRequest request,
            HttpServletResponse response) {
        final InterMineAPI im = InterMineContext.getInterMineAPI();
        new ExternalLinksService(im).service(request, response);
    }
}
