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

import java.util.Date;
import java.util.UUID;

import org.intermine.api.profile.Profile;

public final class DeletionToken {

    private final Profile profile;
    private final Date expiry;
    private final UUID uuid;

    public DeletionToken(Profile profile, UUID uuid, Date expiry) {
        this.profile = profile;
        this.uuid = uuid;
        this.expiry = expiry;
    }

    public Profile getProfile() {
        return profile;
    }

    public Date getExpiry() {
        return expiry;
    }

    public UUID getUUID() {
        return uuid;
    }

}
