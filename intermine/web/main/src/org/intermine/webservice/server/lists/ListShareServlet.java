package org.intermine.webservice.server.lists;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.core.NoServiceException;
import org.intermine.webservice.server.core.WebServiceServlet;

/**
 * @author Alex Kalderimis
 */
public class ListShareServlet extends WebServiceServlet
{

    private static final long serialVersionUID = 1L;

    @Override
    protected WebService getService(Method method) throws NoServiceException {
        switch (method) {
            case GET:
                return new ListShareDetailsService(api);
            case POST:
                return new ListShareCreationService(api);
            case DELETE:
                return new ListShareDeletionService(api);
            default:
                throw new NoServiceException();
        }
    }

}
