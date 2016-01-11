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

import java.util.Date;
import java.util.UUID;

import org.intermine.api.profile.Profile;

/**
 * A class representing a deletion token. This token represents a user's intention
 * to delete their profile.
 * @author Alex Kalderimis
 *
 */
public final class DeletionToken
{

    private final Profile profile;
    private final Date expiry;
    private final UUID uuid;

    /**
     * @param profile The profile this token refers to.
     * @param uuid An identifier.
     * @param expiry When this token expires.
     */
    public DeletionToken(Profile profile, UUID uuid, Date expiry) {
        this.profile = profile;
        this.uuid = uuid;
        this.expiry = expiry;
    }

    /** @return the profile this token refers to **/
    public Profile getProfile() {
        return profile;
    }

    /** @return when this token is valid until **/
    public Date getExpiry() {
        return expiry;
    }

    /** @return the identifier of this token **/
    public UUID getUUID() {
        return uuid;
    }

}
