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
import org.intermine.api.bag.SharingInvite;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.util.Emailer;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.context.MailAction;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;

/**
 * Invite a user to share a list.
 * @author Alex Kalderimis
 *
 */
public class ListSharingInvitationService extends JSONService
{

    private static final Logger LOG = Logger.getLogger(ListSharingInvitationService.class);
    private static final String EMAIL_PROPERTY = "sharing-invite";

    /** @param im The InterMine state object **/
    public ListSharingInvitationService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected String getResultsKey() {
        return "invitation";
    }

    private final class UserInput
    {
        final Profile owner;
        final InterMineBag bag;
        final String invitee;
        final boolean notify;

        UserInput() {
            owner = getAuthenticatedUser();
            String bagName = getRequiredParameter("list");
            bag = owner.getSavedBags().get(bagName);
            if (bag == null) {
                throw new ResourceNotFoundException("You do not own a list called " + bagName);
            }
            invitee = getRequiredParameter("to");
            String sendEmail = getOptionalParameter("notify", "false");
            notify = ("true".equalsIgnoreCase(sendEmail) || "1".equals(sendEmail));
        }
    }

    @Override
    protected void execute() {
        UserInput input = new UserInput();

        SharingInvite invite = SharedBagManager.inviteToShare(input.bag, input.invitee);

        addResultItem(marshallInvite(input, invite), false);

        if (input.notify) {
            notifyInvitee(input, invite);
        }
    }

    private Map<String, Object> marshallInvite(UserInput input, SharingInvite invite) {
        Map<String, Object> toSerialise = new HashMap<String, Object>();
        toSerialise.put("invite-token", invite.getToken());
        toSerialise.put("list", input.bag.getName());
        toSerialise.put("invitee", input.invitee);
        return toSerialise;
    }

    private void notifyInvitee(final UserInput input, final SharingInvite invite) {
        final InterMineBag bag = invite.getBag();
        boolean queued = InterMineContext.queueMessage(new MailAction() {
            @Override
            public void act(Emailer emailer) throws Exception {
                emailer.email(
                    invite.getInvitee(), EMAIL_PROPERTY,
                    input.owner.getName(),
                    bag.getType(), bag.getName(), bag.getSize(),
                    webProperties.getProperty("webapp.baseurl"),
                    webProperties.getProperty("webapp.path"),
                    invite.getToken(),
                    webProperties.getProperty("project.title"));
            }
        });

        if (!queued) {
            LOG.error("Mail queue full, could not send message");
        }
    }

}
