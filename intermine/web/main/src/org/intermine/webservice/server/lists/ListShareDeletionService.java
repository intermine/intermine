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

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.SharedBagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

/** @author Alex Kalderimis **/
public class ListShareDeletionService extends JSONService
{

    private final ProfileManager pm;
    private final SharedBagManager sbm;

    /** @param im The InterMine state object **/
    public ListShareDeletionService(InterMineAPI im) {
        super(im);
        pm = im.getProfileManager();
        sbm = SharedBagManager.getInstance(pm);
    }

    private final class UserInput
    {
        final InterMineBag bag;
        final Profile owner;
        final Profile recipient;

        UserInput() {
            owner = getPermission().getProfile();
            if (!owner.isLoggedIn()) {
                throw new ServiceForbiddenException("Not authenticated.");
            }
            String bagName = request.getParameter("list");
            if (StringUtils.isBlank(bagName)) {
                throw new BadRequestException("Missing parameter: 'list'");
            }
            if (!"*".equals(bagName)) {
                bag = owner.getSavedBags().get(bagName);
                if (bag == null) {
                    throw new ResourceNotFoundException(
                            "The value of the 'list' parameter is not a list you own");
                }
            } else {
                bag = null; // meaning wildcard
            }
            String recipientName = request.getParameter("with");
            if (StringUtils.isBlank(recipientName)) {
                throw new BadRequestException("Missing parameter: 'with'");
            }
            if (!"*".equals(recipientName)) {
                recipient = pm.getProfile(recipientName);
                if (recipient == null) {
                    throw new ResourceNotFoundException(
                            "The value of the 'with' parameter is not a user in the data-base");
                }
            } else {
                recipient = null; // meaning wildcard
            }
            if (recipient == null && bag == null) {
                throw new BadRequestException("Too many wildcards");
            }
        }
    }

    @Override
    protected void execute() throws Exception {
        UserInput input = new UserInput();

        if (input.bag != null && input.recipient != null) {
            // remove just a specific share.
            sbm.unshareBagWithUser(input.bag, input.recipient.getName());
        } else if (input.recipient == null) {
            sbm.unshareBagWithAllUsers(input.bag);
        } else {
            sbm.unshareAllBagsFromUser(input.owner, input.recipient);
        }
    }

}
