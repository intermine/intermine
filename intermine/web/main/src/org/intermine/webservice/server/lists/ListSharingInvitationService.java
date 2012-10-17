package org.intermine.webservice.server.lists;

import java.util.HashMap;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.SharedBagManager;
import org.intermine.api.bag.SharingInvite;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.Emailer;
import org.intermine.web.context.InterMineContext;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.exceptions.InternalErrorException;

public class ListSharingInvitationService extends JSONService {

    private final static String EMAIL_PROPERTY = "sharing-invite";

    public ListSharingInvitationService(InterMineAPI im) {
        super(im);
    }

    protected String getResultsKey() {
        return "invitation";
    }

    private final class UserInput {
        final Profile owner;
        final InterMineBag bag;
        final String invitee;
        final boolean notify;

        UserInput() throws ServiceException {
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
    protected void execute() throws ServiceException {
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

    private void notifyInvitee(final UserInput input, final SharingInvite invite) throws ServiceException {
        final InterMineBag bag = invite.getBag();
        final Emailer emailer = InterMineContext.getEmailer();
        try {
            emailer.email(
                invite.getInvitee(), EMAIL_PROPERTY,
                input.owner.getName(),
                bag.getType(), bag.getName(), bag.getSize(),
                webProperties.getProperty("webapp.baseurl"),
                webProperties.getProperty("webapp.path"),
                invite.getToken(),
                webProperties.getProperty("project.title"));
        } catch (ObjectStoreException e) {
            throw new InternalErrorException("Could not retrieve the size of the bag", e);
        } catch (AddressException e) {
            throw new BadRequestException("I cannot send an email to this invitee, as this is not a valid email address", e);
        } catch (MessagingException e) {
            throw new InternalErrorException("Could not send invitation (" + invite + ")", e);
        }
    }

}
