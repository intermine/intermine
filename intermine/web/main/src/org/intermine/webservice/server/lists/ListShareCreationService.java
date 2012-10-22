package org.intermine.webservice.server.lists;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.mail.MessagingException;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.SharedBagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.UserAlreadyShareBagException;
import org.intermine.api.profile.UserNotFoundException;
import org.intermine.api.profile.UserPreferences;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.Emailer;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.Constants;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.intermine.webservice.server.exceptions.MissingParameterException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

public class ListShareCreationService extends JSONService {

    private final static Logger LOG = Logger.getLogger(ListShareCreationService.class);
    private final ProfileManager pm;
    private final SharedBagManager sbm;

    public ListShareCreationService(InterMineAPI im) {
        super(im);
        pm = im.getProfileManager();
        sbm = SharedBagManager.getInstance(pm);
    }
    
    private final class UserInput {
        final Profile owner;
        final Profile recipient;
        final InterMineBag bag;
        final Boolean notify;

        UserInput() throws ServiceException {
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
                throw new ResourceNotFoundException("The value of the 'list' parameter is not the name of a list you own");
            }
            String recipientName = getRequiredParameter("with");
            // Is this dangerous? it allows users to
            // scrape for usernames. But the you can do the same
            // thing in the registration service...
            recipient = pm.getProfile(recipientName);
            if (recipient == null || recipient.prefers(Constants.HIDDEN)) {
                throw new ResourceNotFoundException("The value of the 'with' parameter is not the name of user you can share lists with");
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
         Emailer emailer = InterMineContext.getEmailer();

         try {
             sbm.shareBagWithUser(input.bag, input.recipient.getUsername());
         } catch (UserAlreadyShareBagException e) {
             throw new BadRequestException("This bag is already shared with this user", e);
         }

         Map<String, Object> data = new HashMap<String, Object>();
         data.put(input.bag.getName(), sbm.getUsersWithAccessToBag(input.bag));

         addResultItem(data, false);

         if (input.notify && !input.recipient.prefers(Constants.NO_SPAM)) {
             try {
                 emailer.informUserOfNewSharedBag(input.recipient.getEmailAddress(), input.owner, input.bag);
             } catch (Exception e) {
                 LOG.warn("Could not email recipient: " + input.recipient, e);
             }
         }
    }

}
