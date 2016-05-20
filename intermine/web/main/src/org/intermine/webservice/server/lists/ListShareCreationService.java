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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.SharedBagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.UserAlreadyShareBagException;
import org.intermine.util.Emailer;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.context.MailAction;
import org.intermine.web.logic.Constants;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

/** @author Alex Kalderimis **/
public class ListShareCreationService extends JSONService
{

    private static final Logger LOG = Logger.getLogger(ListShareCreationService.class);
    private final ProfileManager pm;
    private final SharedBagManager sbm;

    /** @param im The InterMine state object. **/
    public ListShareCreationService(InterMineAPI im) {
        super(im);
        pm = im.getProfileManager();
        sbm = SharedBagManager.getInstance(pm);
    }

    private final class UserInput
    {
        private static final String CANT_SHARE_WITH_THEM
            = "The value of the 'with' parameter is not the name of user you can share lists with";
        private static final String NOT_YOUR_LIST
            = "The value of the 'list' parameter is not the name of a list you own";
        final Profile owner;
        final Profile recipient;
        final InterMineBag bag;
        final Boolean notify;

        UserInput() {
            owner = getPermission().getProfile();
            if (!owner.isLoggedIn()) {
                throw new ServiceForbiddenException("Not authenticated.");
            }
            Map<String, InterMineBag> bags = owner.getSavedBags();
            if (bags.isEmpty()) {
                throw new BadRequestException("You do not have any lists to share");
            }

            String bagName = getRequiredParameter("list");
            bag = bags.get(bagName);
            if (bag == null) {
                throw new ResourceNotFoundException(NOT_YOUR_LIST);
            }
            String recipientName = getRequiredParameter("with");
            // Is this dangerous? it allows users to
            // scrape for usernames. But the you can do the same
            // thing in the registration service...
            recipient = pm.getProfile(recipientName);
            if (recipient == null || recipient.prefers(Constants.HIDDEN)) {
                // Maybe insert sleep here to prevent scraping??
                throw new ResourceNotFoundException(CANT_SHARE_WITH_THEM);
            }

            String notifyStr = getOptionalParameter("notify", "false");
            notify = Boolean.parseBoolean(notifyStr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResultsKey() {
        return "share";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void execute() throws Exception {
        UserInput input = new UserInput();

        try {
            sbm.shareBagWithUser(input.bag, input.recipient.getUsername());
        } catch (UserAlreadyShareBagException e) {
            throw new BadRequestException("This bag is already shared with this user", e);
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put(input.bag.getName(), sbm.getUsersWithAccessToBag(input.bag));

        if (input.notify && !input.recipient.prefers(Constants.NO_SPAM)) {
            MailAction action = new InformUserOfNewShare(
                    input.recipient.getEmailAddress(), input.owner, input.bag);
            if (!InterMineContext.queueMessage(action)) {
                LOG.error("Could not send email message");
            }
        }

        addResultItem(data, false);

    }

    private class InformUserOfNewShare implements MailAction
    {

        private final String to;
        private final Profile owner;
        private final InterMineBag bag;

        InformUserOfNewShare(String to, Profile owner, InterMineBag bag) {
            this.to = to;
            this.owner = owner;
            this.bag = bag;
        }

        @Override
        public void act(Emailer emailer) throws Exception {
            emailer.informUserOfNewSharedBag(to, owner, bag);
        }
    }

}
