package org.intermine.webservice.server.lists;

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
import org.intermine.webservice.server.exceptions.UnauthorizedException;

/**
 * A Class that insists on being authenticated.
 * @author Alex Kalderimis
 */
public abstract class AuthenticatedListService extends AbstractListService
{

    /**
     * Constructor.
     * @param api The InterMine application object.
     */
    public AuthenticatedListService(InterMineAPI api) {
        super(api);
    }

    @Override
    protected void validateState() {
        super.validateState();
        if (!isAuthenticated()) {
            throw new UnauthorizedException(
                "All requests to list operation services must"
                + " be authenticated."
            );
        }
    }
}
