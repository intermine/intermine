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

/** @author Alex Kalderimis **/
public class ListSharingInvitationServlet extends WebServiceServlet
{

    private static final long serialVersionUID = -6661704993993686163L;

    @Override
    protected WebService getService(Method method) throws NoServiceException {
        switch (method) {
            case GET:
                return new ListSharingInvitationDetailsService(api);
            case POST:
                return new ListSharingInvitationService(api);
            case PUT:
                return new ListSharingInvitationAcceptanceService(api);
            case DELETE:
                return new ListSharingInvitationDeletionService(api);
            default:
                throw new NoServiceException();
        }
    }
}
