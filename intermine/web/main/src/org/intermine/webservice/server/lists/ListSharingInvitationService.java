package org.intermine.webservice.server.lists;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.SharedBagManager;
import org.intermine.api.bag.SharingInvite;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.web.context.InterMineContext;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.MissingParameterException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

public class ListSharingInvitationService extends JSONService {

    private static final Logger LOG = Logger.getLogger(ListSharingInvitationService.class);
    
    public ListSharingInvitationService(InterMineAPI im) {
        super(im);
    }
    
    protected String getResultsKey() {
        return "invitation";
    }

    @Override
    protected void execute() throws ServiceException {
        Profile p = getPermission().getProfile();
        if (!p.isLoggedIn()) {
            throw new ServiceForbiddenException("You must be logged in to use this service");
        }
        String bagName = request.getParameter("list");
        if (StringUtils.isBlank(bagName)) {
            throw new MissingParameterException("list");
        }
        InterMineBag bag = p.getSavedBags().get(bagName);
        if (bag == null) {
            throw new ResourceNotFoundException("You do not own a list called " + bagName);
        }
        String invitee = request.getParameter("to");
        if (StringUtils.isBlank(invitee)) {
            throw new MissingParameterException("to");
        }
        SharingInvite invite = SharedBagManager.inviteToShare(bag, invitee);
        
        String sendEmail = request.getParameter("notify");
        if ("true".equalsIgnoreCase(sendEmail) || "1".equals(sendEmail)) {
            notifyInvitee(invite);
        }
        Map<String, Object> toSerialise = new HashMap<String, Object>();
        toSerialise.put("invite-token", invite.getToken());
        toSerialise.put("list", bag.getName());
        toSerialise.put("invitee", invitee);
        
        addResultItem(toSerialise, false);
    }
    
    private final static String EMAIL_PROPERTY = "sharing-invite";

    private void notifyInvitee(final SharingInvite invite) {
        (new Thread() { // Send message in background.
            @Override
            public void run() {
                final InterMineBag bag = invite.getBag();
                final Profile owner = getPermission().getProfile();
                try {
                    InterMineContext.getEmailer().email(
                        invite.getInvitee(), EMAIL_PROPERTY,
                        owner.getName(),
                        bag.getType(), bag.getName(), bag.getSize(),
                        webProperties.getProperty("webapp.baseurl"),
                        webProperties.getProperty("webapp.path"),
                        invite.getToken(),
                        webProperties.getProperty("project.title"));
                } catch (Exception e) {
                    LOG.error("Could not send invitation (" + invite + ")", e);
                }
            }        
        }).start();
    }

}
