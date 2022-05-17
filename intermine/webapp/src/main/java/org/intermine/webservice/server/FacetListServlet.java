package org.intermine.webservice.server;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.web.context.InterMineContext;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A web service for returning all the facet list
 * @author arunans23
 *
 */
public class FacetListServlet extends HttpServlet
{
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        FacetListService facetListService
                = new FacetListService(InterMineContext.getInterMineAPI());
        facetListService.service(request, response);
    }
}
