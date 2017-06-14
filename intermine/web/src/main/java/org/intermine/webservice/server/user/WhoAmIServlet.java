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

/** @author Alex Kalderimis **/
public class WhoAmIServlet extends WebServiceServlet
{

    private static final long serialVersionUID = -8186762486810978232L;

    @Override
    public WebService getService(Method method) throws NoServiceException {
        if (Method.GET == method) {
            return new WhoAmIService(api);
        } else {
            throw new NoServiceException();
        }
    }

}
