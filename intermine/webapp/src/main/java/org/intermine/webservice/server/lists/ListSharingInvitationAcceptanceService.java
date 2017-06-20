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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.SharedBagManager;
import org.intermine.api.bag.SharingInvite.NotFoundException;
import org.intermine.api.bag.SharingInvite;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.UserAlreadyShareBagException;
import org.intermine.api.profile.UserNotFoundException;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.Emailer;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.context.MailAction;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

/** @author Alex Kalderimis **/
public class ListSharingInvitationAcceptanceService extends JSONService
{

    private static final Logger LOG =
        Logger.getLogger(ListSharingInvitationAcceptanceService.class);
    private static final Set<String> ACCEPTABLE_ACCEPTANCES =
        new HashSet<String>(Arrays.asList("true", "false"));

    private final SharedBagManager sbm;

    /** @param im The InterMine state object **/
    public ListSharingInvitationAcceptanceService(InterMineAPI im) {
        super(im);
        sbm = SharedBagManager.getInstance(im.getProfileManager());
    }

    @Override
    protected String getResultsKey() {
        return "list";
    }

    /**
     * Parameter object, holding the storage of the parsed parameters, and the
     * logic for weedling them out of the HTTP parameters.
     */
    private final class UserInput
    {
        private static final String NOT_ACCEPTABLE
            = "The value of the 'accepted' parameter must be one of ";
        final boolean accepted;
        final SharingInvite invite;
        final Profile accepter;
        final boolean notify;

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
            String isAccepted = request.getParameter("accepted");
            if (StringUtils.isBlank(isAccepted)) {
                throw new BadRequestException("Missing required parameter: 'accepted'");
            } else if (!ACCEPTABLE_ACCEPTANCES.contains(isAccepted.toLowerCase())) {
                throw new BadRequestException(NOT_ACCEPTABLE + ACCEPTABLE_ACCEPTANCES);
            }
            accepted = Boolean.parseBoolean(isAccepted);

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

            String sendEmail = request.getParameter("notify");
            if (StringUtils.isNotBlank(sendEmail) && "true".equalsIgnoreCase(sendEmail)) {
                notify = true;
            } else {
                notify = false;
            }
        }
    }

    @Override
    protected void execute() throws NotFoundException {
        UserInput input = new UserInput();

        // Do the acceptance.
        try {
            sbm.resolveInvitation(input.invite, input.accepter, input.accepted);
        } catch (UserNotFoundException e) {
            throw new ServiceException(
                "Inconsistent state: p.isLoggedIn() but not found in DB");
        } catch (UserAlreadyShareBagException e) {
            LOG.warn("User accepted an invitation to a list they already have access to", e);
        }


        if (input.notify) {
            notifyOwner(input.invite);
        }

        // Send back information about the new list, if they accepted.
        if (input.accepted) {
            JSONListFormatter formatter = new JSONListFormatter(im, input.accepter, false);
            addResultItem(formatter.bagToMap(input.invite.getBag()), false);
        } else {
            addResultItem(new HashMap<String, Object>(), false);
        }
    }

    private void notifyOwner(final SharingInvite invite) {
        final ProfileManager pm = im.getProfileManager();
        final Profile receiver = getPermission().getProfile();
        final Profile owner = pm.getProfile(invite.getBag().getProfileId());

        boolean queued = InterMineContext.queueMessage(new MailAction() {
            @Override
            public void act(Emailer emailer) throws Exception {
                emailer.email(
                        owner.getEmailAddress(), "was-accepted",
                        invite.getCreatedAt(), invite.getInvitee(), invite.getBag(),
                        receiver.getUsername(), webProperties.getProperty("project.title"));
            }
        });

        if (!queued) {
            LOG.error("Mail queue full, could not send message");
        }
    }

}
