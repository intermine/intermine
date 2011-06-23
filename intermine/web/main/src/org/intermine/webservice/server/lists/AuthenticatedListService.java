package org.intermine.webservice.server.lists;

import org.intermine.api.InterMineAPI;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

/**
 * A Class that insists on being authenticated.
 */
public abstract class AuthenticatedListService extends AbstractListService
{

    public AuthenticatedListService(InterMineAPI api) {
        super(api);
    }

    @Override
    protected void validateState() {
        if (!isAuthenticated()) {
            throw new ServiceForbiddenException("All requests to list operation services must"
                    + " be authenticated.");
        }
    }
}

