package org.intermine.webservice.server.user;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.ProfileManager;
import org.intermine.util.MailUtils;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;

/**
 * This service sends a password change email to the user email address
 * @author Daniela Butano
 *
 */
public class RequestPswResetService extends JSONService
{
    private static final Logger LOG = Logger.getLogger(RequestPswResetService.class);

    /**
     * Constructor
     * @param im A reference to the InterMine API settings bundle
     */
    public RequestPswResetService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        String username = getRequiredParameter("email");
        String redirectUrl = getRequiredParameter("redirectUrl");

        ProfileManager pm = im.getProfileManager();
        try {
            String token = pm.createPasswordChangeToken(username);
            MailUtils.emailPasswordToken(username, redirectUrl + "?token=" + token,
                    webProperties);
        } catch (IllegalArgumentException ex ) {
            throw new BadRequestException("Email " + username + " not found");
        }
    }
}
