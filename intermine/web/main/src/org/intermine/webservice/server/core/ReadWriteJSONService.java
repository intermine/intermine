package org.intermine.webservice.server.core;

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
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

/**
 * Convenience for services that need to combine JSON service behaviour
 * with strict RW authorisation.
 *
 * @author Alex Kalderimis
 *
 */
public abstract class ReadWriteJSONService extends JSONService
{

    private static final String DENIAL_MSG = "Access denied.";

    /**
     * Construct a read/write service.
     * @param im The InterMine state object.
     */
    public ReadWriteJSONService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void validateState() {
        if (!isAuthenticated() || getPermission().isRO()) {
            throw new ServiceForbiddenException(DENIAL_MSG);
        }
    }

}
