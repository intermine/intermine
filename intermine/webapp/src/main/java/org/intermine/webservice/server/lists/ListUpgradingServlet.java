package org.intermine.webservice.server.lists;

/*
 * Copyright (C) 2002-2022 FlyMine
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

/** @author Daniela Butano **/
public class ListUpgradingServlet extends WebServiceServlet
{
    @Override
    protected WebService getService(Method method) throws NoServiceException {
        if (Method.DELETE == method) {
            throw new NoServiceException();
        }
        return new ListUpgradingService(api);
    }
}
