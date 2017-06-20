package org.intermine.webservice.server.user;

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
 * Servlet for handing off requests to the a NewUserService.
 * @author Alex Kalderimis.
 *
 */
public class NewUserServlet extends WebServiceServlet
{
    private static final long serialVersionUID = 2247791931782821682L;

    @Override
    protected WebService getService(Method method) throws NoServiceException {
        switch (method) {
            case POST:
                return new NewUserService(api);
            default:
                throw new NoServiceException();
        }
    }

}
