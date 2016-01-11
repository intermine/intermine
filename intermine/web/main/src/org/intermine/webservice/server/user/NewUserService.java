package org.intermine.webservice.server.user;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import org.directwebremoting.util.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.util.Emailer;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.context.MailAction;
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
        ProfileManager pm = im.getProfileManager();
        NewUserInput input = new NewUserInput();

        pm.createNewProfile(input.getUsername(), input.getPassword());

        JSONObject user = new JSONObject();
        user.put("username", input.getUsername());

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

        Profile p = pm.getProfile(input.getUsername());
        if (p == null) {
            throw new ServiceException("Creating profile failed");
        }
        user.put("temporaryToken", pm.generate24hrKey(p));

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
            username = request.getParameter("name");
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

}
