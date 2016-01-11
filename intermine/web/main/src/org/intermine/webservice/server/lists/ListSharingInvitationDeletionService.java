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

import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.SharedBagManager;
import org.intermine.api.bag.SharingInvite;
import org.intermine.api.bag.SharingInvite.NotFoundException;
import org.intermine.api.profile.Profile;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

/** @author Alex Kalderimis **/
public class ListSharingInvitationDeletionService extends JSONService
{

    @SuppressWarnings("unused")
    private final SharedBagManager sbm;

    /**
     * Construct a service handler.
     * @param im The InterMine state object.
     */
    public ListSharingInvitationDeletionService(InterMineAPI im) {
        super(im);
        // Needs getting, as this ensures the tables are all set up.
        sbm = SharedBagManager.getInstance(im.getProfileManager());
    }

    /**
     * Parameter object, holding the storage of the parsed parameters, and the
     * logic for weedling them out of the HTTP parameters.
     */
    private final class UserInput

    {
        final SharingInvite invite;
        final Profile accepter;

        /**
         * Do the dance of parameter parsing and validation.
         */
        UserInput() {
            accepter = getPermission().getProfile();

            if (!accepter.isLoggedIn()) {
                throw new ServiceForbiddenException("You must be logged in");
            }
            String token, pathInfo = request.getPathInfo();
            if (pathInfo != null && pathInfo.matches("/[^/]{20}")) {
                token = pathInfo.substring(1, 21);
            } else {
                token = request.getParameter("uid");
            }
            if (StringUtils.isBlank(token)) {
                throw new BadRequestException("Missing required parameter: 'uid'");
            }

            try {
                invite = SharingInvite.getByToken(im, token);
            } catch (SQLException e) {
                throw new ServiceException("Error retrieving invitation", e);
            } catch (ObjectStoreException e) {
                throw new ServiceException("Corrupt invitation", e);
            } catch (NotFoundException e) {
                throw new ResourceNotFoundException("invitation does not exist", e);
            }
            if (invite == null) {
                throw new ResourceNotFoundException("Could not find invitation");
            }
        }
    }

    @Override
    protected void execute() throws Exception {
        UserInput input = new UserInput();

        try {
            input.invite.delete();
        } catch (NotFoundException e) {
            throw new ResourceNotFoundException("Error deleting token.", e);
        }

    }

}
