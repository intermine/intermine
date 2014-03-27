package org.intermine.webservice.server.user;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.UUID;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;
import org.intermine.webservice.server.user.DeletionTokens.TokenExpired;

public class DeletionTokenInfoService extends NewDeletionTokenService {

    private String uuid;

    public DeletionTokenInfoService(InterMineAPI im, String uid) {
        super(im);
        this.uuid = uid;
    }

    @Override
    protected void execute() {
        DeletionToken token = getToken();
        serveToken(token);
    }

    protected DeletionToken getToken() {
        DeletionToken token;
        Profile profile = getPermission().getProfile();
        try {
            UUID key = UUID.fromString(uuid);
            token = tokenFactory.retrieveToken(key);
        } catch (IllegalArgumentException  e) {
            throw new BadRequestException(uuid + " is not a deletion token.");
        } catch (TokenExpired e) {
            throw new BadRequestException("token expired.");
        }
        if (!profile.equals(token.getProfile())) {
            throw new ServiceForbiddenException("Access denied");
        }
        return token;
    }

}
