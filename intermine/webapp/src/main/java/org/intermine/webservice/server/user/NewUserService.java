package org.intermine.webservice.server.user;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.directwebremoting.util.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.util.NameUtil;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.Emailer;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.context.MailAction;
import org.intermine.web.logic.profile.ProfileMergeIssues;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.core.RateLimitHistory;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.exceptions.RateLimitException;
import org.intermine.webservice.server.output.JSONFormatter;
import org.json.JSONObject;

/**
 * Service for creating a new user. Requires a new, unique username, and a non-blank
 * password.
 * @author Alex Kalderimis.
 *
 */
public class NewUserService extends JSONService
{

    private static final String DEFAULTING_TO_1000PH
        = "Configured new user rate limit is not a valid integer. Defaulting to 1000 per hour";
    private static final Logger LOG = Logger.getLogger(NewUserService.class);
    private int maxNewUsersPerAddressPerHour = 1000;
    private static RateLimitHistory requestHistory = null;

    /**
     * Constructor.
     * @param im The InterMine API object.
     */
    public NewUserService(InterMineAPI im) {
        super(im);
        if (requestHistory == null) {
            Properties webProperties = InterMineContext.getWebProperties();
            String rateLimit = webProperties.getProperty("webservice.newuser.ratelimit");
            if (rateLimit != null) {
                try {
                    maxNewUsersPerAddressPerHour = Integer.valueOf(rateLimit.trim()).intValue();
                } catch (NumberFormatException e) {
                    LOG.error(DEFAULTING_TO_1000PH, e);
                    maxNewUsersPerAddressPerHour = 1000;
                }
            }
            requestHistory = new RateLimitHistory((60 * 60), maxNewUsersPerAddressPerHour);
        }
    }

    @Override
    protected void validateState() {
        super.validateState();
        final String ipAddr = request.getRemoteAddr();
        if (!requestHistory.isWithinLimit(ipAddr)) {
            throw new RateLimitException(ipAddr, maxNewUsersPerAddressPerHour);
        }
        // Record this request...
        requestHistory.recordRequest(ipAddr);
    }

    @Override
    protected void execute() throws Exception {
        NewUserInput input = new NewUserInput();
        Profile currentProfile = getPermission().getProfile();

        ProfileManager pm = im.getProfileManager();
        pm.createNewProfile(input.getUsername(), input.getPassword());
        Profile newProfile = pm.getProfile(input.getUsername());
        if (newProfile == null) {
            throw new ServiceException("Creating a new profile failed");
        }

        ProfileMergeIssues issues = null;
        if (currentProfile != null) {
            //attach the anonymously created lists to the new profile
            issues = attachAnonSavedBags(currentProfile, newProfile);
        }

        JSONObject user = new JSONObject();
        user.put("username", input.getUsername());
        user.put("renamedLists", new JSONObject(issues.getRenamedBags()));

        MailAction welcomeMessage = new WelcomeAction(input.getUsername());
        if (!InterMineContext.queueMessage(welcomeMessage)) {
            LOG.error("Mail queue capacity exceeded. Not sending welcome message");
        }

        String mailingList = null;
        if (input.subscribeToList()) {
            mailingList = getProperty("mail.mailing-list");
            MailAction subscribe = new SubscribeAction(input.getUsername());
            if (!InterMineContext.queueMessage(subscribe)) {
                LOG.error("Mail queue capacity exceeded. Not sending subscription message");
            }
        }
        user.put("subscribedToList", mailingList != null);
        user.put("mailingList", mailingList);
        user.put("temporaryToken", pm.generate24hrKey(newProfile));

        output.addResultItem(Arrays.asList(user.toString()));
    }

    @Override
    protected Map<String, Object> getHeaderAttributes() {
        Map<String, Object> retval = super.getHeaderAttributes();
        retval.put(JSONFormatter.KEY_INTRO, "\"user\":");
        return retval;
    }

    private static class WelcomeAction implements MailAction
    {

        private final String to;

        WelcomeAction(String to) {
            this.to = to;
        }

        @Override
        public void act(Emailer emailer) throws Exception {
            emailer.welcome(to);
        }
    }

    private static class SubscribeAction implements MailAction
    {

        private final String to;

        SubscribeAction(String to) {
            this.to = to;
        }

        @Override
        public void act(Emailer emailer) throws Exception {
            emailer.subscribeToList(to);
        }
    }

    private class NewUserInput
    {
        private static final String BAD_REQ_MSG = "missing parameters. name and password required";
        private static final String USER_EXISTS_MSG = "There is already a user with that name";
        private final String username;
        private final String password;
        private final boolean wantsSpam;

        NewUserInput() {
            username = request.getParameter("name").toLowerCase();
            password = request.getParameter("password");
            wantsSpam = Boolean.parseBoolean(request.getParameter("subscribe-to-list"));
            validate();
        }

        boolean subscribeToList() {
            return wantsSpam;
        }

        String getUsername() {
            return username;
        }

        String getPassword() {
            return password;
        }

        private void validate() {
            if (isBlank(username) || isBlank(password)) {
                throw new BadRequestException(BAD_REQ_MSG);
            }

            if (im.getProfileManager().hasProfile(username)) {
                throw new BadRequestException(USER_EXISTS_MSG);
            }
        }
    }

    /**
     * Attach the  anonymously created list to the new user
     *
     * @param fromProfile The profile to take information from.
     * @param newProfile The new profile to attach into.
     * @return A map of bags, from old name to new name.
     */
    private ProfileMergeIssues attachAnonSavedBags(Profile fromProfile, Profile newProfile) {
        Map<String, InterMineBag> mergeBags = Collections.emptyMap();
        ProfileMergeIssues issues = new ProfileMergeIssues();
        if (fromProfile != null) {
            mergeBags = fromProfile.getSavedBags();
        }

        // attach anonymous bags to the new profile
        for (Map.Entry<String, InterMineBag> entry : mergeBags.entrySet()) {
            InterMineBag bag = entry.getValue();
            // Make sure the userId gets set to be the profile one
            try {
                bag.setProfileId(newProfile.getUserId());
                String name = NameUtil.validateName(newProfile.getSavedBags().keySet(),
                        entry.getKey());
                if (!entry.getKey().equals(name)) {
                    issues.addRenamedBag(entry.getKey(), name);
                }
                bag.setName(name);
                newProfile.saveBag(name, bag);
            } catch (ObjectStoreException iex) {
                throw new RuntimeException(iex.getMessage());
            }
        }
        return issues;
    }

}
