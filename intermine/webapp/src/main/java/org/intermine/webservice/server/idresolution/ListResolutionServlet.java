package org.intermine.webservice.server.idresolution;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.core.NoServiceException;

/**
 * Route requests for list resolution.
 * @author Daniela Butano
 *
 */
public class ListResolutionServlet extends IdResolutionServlet
{
    @Override
    protected WebService getService(Method method) throws NoServiceException {
        switch (method) {
            case POST:
                return new ListResolutionService(api);
            case DELETE:
                return new JobRemovalService(api);
            default:
                throw new NoServiceException();
        }
    }
}
