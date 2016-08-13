package org.intermine.webservice.server.template;

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
 * Runs the template-to-list service to run templates and save them as lists.
 * @author Alex Kalderimis
 *
 */
public class TemplateToListServlet extends WebServiceServlet
{

    private static final long serialVersionUID = 7734594953300950226L;

    @Override
    protected WebService getService(Method method) throws NoServiceException {
        switch (method) {
            case GET:
                return new TemplateToListService(api);
            case POST:
                return new TemplateToListService(api);
            default:
                throw new NoServiceException();
        }
    }
}
