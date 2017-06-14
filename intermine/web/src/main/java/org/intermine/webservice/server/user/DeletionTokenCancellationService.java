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

import org.intermine.api.InterMineAPI;

/**
 * A service that cancels the profile deletion process by deleting the
 * token that could be used to complete it.
 * @author Alex Kalderimis
 *
 */
public class DeletionTokenCancellationService extends DeletionTokenInfoService
{

    /**
     * @param im The InterMine state object
     * @param uuid The identifier of the token
     */
    public DeletionTokenCancellationService(InterMineAPI im, String uuid) {
        super(im, uuid);
    }

    @Override
    protected void execute() {
        DeletionToken token = getToken();
        tokenFactory.removeToken(token);
    }

}
