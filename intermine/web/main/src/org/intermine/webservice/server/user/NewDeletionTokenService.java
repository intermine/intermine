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

import java.util.HashMap;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.webservice.server.core.ISO8601DateFormat;
import org.intermine.webservice.server.core.ReadWriteJSONService;

/**
 * A service that issues a deletion token to a user who intends to
 * delete their profile.
 * @author Alex Kalderimis
 *
 */
public class NewDeletionTokenService extends ReadWriteJSONService
{

    protected final DeletionTokens tokenFactory;

    /**
     * @param im The InterMine state object.
     */
    public NewDeletionTokenService(InterMineAPI im) {
        super(im);
        this.tokenFactory = DeletionTokens.getInstance();
    }

    @Override
    protected String getResultsKey() {
        return "token";
    }

    @Override
    protected void execute() throws Exception {
        int lifeSpan = getIntParameter("validity", 60);
        Profile profile = getPermission().getProfile();
        DeletionToken token = tokenFactory.createToken(profile, lifeSpan);
        serveToken(token);
    }

    /**
     * Serve a token to the outside world.
     * @param token The token to return.
     */
    protected void serveToken(DeletionToken token) {
        Map<String, Object> info = new HashMap<String, Object>();

        info.put("uuid", token.getUUID().toString());
        info.put("expiry", ISO8601DateFormat.getFormatter().format(token.getExpiry()));
        info.put("secondsRemaining",
                (token.getExpiry().getTime() - System.currentTimeMillis()) / 1000);

        this.addResultItem(info, false);
    }

}
