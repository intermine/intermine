package org.intermine.webservice.server.search;

/*
 * Copyright (C) 2002-2015 FlyMine
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

/** @author Alex Kalderimis **/
public class QuickSearchServlet extends WebServiceServlet
{
    private static final long serialVersionUID = -5506185356973283525L;

    @Override
    protected WebService getService(Method method) throws NoServiceException {
        switch (method) {
            case GET:
                return new QuickSearch(api);
            case POST:
                return new QuickSearch(api);
            default:
                throw new NoServiceException();
        }
    }

}
