package org.intermine.webservice.server;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.webservice.server.core.NoServiceException;
import org.intermine.webservice.server.core.WebServiceServlet;

import javax.servlet.ServletContext;

/**
 * A web service for returning facet related to particular query
 * @author arunans23
 *
 */
public class FacetServlet extends WebServiceServlet
{
    @Override
    protected WebService getService(Method method) throws NoServiceException {
        ServletContext ctx = this.getServletContext();
        switch (method) {
            case GET:
                return new FacetService(api);
            case POST:
                return new FacetService(api);
            default:
                throw new NoServiceException();

        }
    }
}
