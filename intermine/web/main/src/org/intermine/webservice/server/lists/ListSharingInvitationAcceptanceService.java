package org.intermine.webservice.server.lists;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.mail.MessagingException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.SharedBagManager;
import org.intermine.api.bag.SharingInvite;
import org.intermine.api.bag.SharingInvite.NotFoundException;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.UserAlreadyShareBagException;
import org.intermine.api.profile.UserNotFoundException;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.Emailer;
import org.intermine.web.context.InterMineContext;
import org.intermine.webservice.client.exceptions.InternalErrorException;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

public class ListSharingInvitationAcceptanceService extends JSONService {

    private static final Logger LOG = 
        Logger.getLogger(ListSharingInvitationAcceptanceService.class);
    private static final Set<String> acceptableAcceptances =
        new HashSet<String>(Arrays.asList("true", "false"));
    
    private final SharedBagManager sbm;
    
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
    private final class UserInput {
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
            } else if (!acceptableAcceptances.contains(isAccepted.toLowerCase())) {
                throw new BadRequestException("The value of the 'accepted' parameter must be one of " + acceptableAcceptances);
            }
            accepted = Boolean.parseBoolean(isAccepted);
            
            try {
                invite = SharingInvite.getByToken(im, token);
            } catch (SQLException e) {
                throw new InternalErrorException("Error retrieving invitation", e);
            } catch (ObjectStoreException e) {
                throw new InternalErrorException("Corrupt invitation", e);
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
    protected void execute() throws ServiceException, NotFoundException {
        UserInput input = new UserInput();

        // Do the acceptance.
        try {
            sbm.resolveInvitation(input.invite, input.accepter, input.accepted);
        } catch (UserNotFoundException e) {
            throw new InternalErrorException(
                "Inconsistent state: p.isLoggedIn() but not found in DB");
        } catch (UserAlreadyShareBagException e) {    
            LOG.warn("User accepted an invitation to a list they already have access to", e);
        }
        // Send back information about the new list, if they accepted.
        if (input.accepted) {
            JSONListFormatter formatter = new JSONListFormatter(im, input.accepter, false);
            addResultItem(formatter.bagToMap(input.invite.getBag()), false);
        } else {
            addResultItem((Map) Collections.emptyMap(), false);
        }

        if (input.notify) {
            notifyOwner(input.invite);
        }
    }
    
    private void notifyOwner(SharingInvite invite) {
        Emailer emailer = InterMineContext.getEmailer();
        ProfileManager pm = im.getProfileManager();
        Profile receiver = getPermission().getProfile();
        Profile owner = pm.getProfile(invite.getBag().getProfileId());

        try {
        emailer.email(
                owner.getEmailAddress(), "was-accepted",
                invite.getCreatedAt(), invite.getInvitee(), invite.getBag(),
                receiver.getUsername(), webProperties.getProperty("project.title"));
        } catch (MessagingException e) {
            LOG.error("Could not notify owner", e);
        }
    }

}
