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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.intermine.api.profile.Profile;

/**
 * A class that manages deletion tokens for the lifespan of a webapp.
 * @author Alex Kalderimis
 *
 */
public final class DeletionTokens
{

    private static final int MINIMUM_LIFE = 10; // 10 Seconds.
    private static final int MAXIMUM_LIFE = 60 * 60 * 24; // One day.

    private Map<UUID, DeletionToken> tokens = new HashMap<UUID, DeletionToken>();

    private static DeletionTokens instance = new DeletionTokens();

    /** @return a DeletionTokens manager **/
    public static DeletionTokens getInstance() {
        return instance;
    }

    private DeletionTokens() {
        // Do not construct.
    }

    /**
     * Create a new DeletionToken
     * @param profile The profile this token refers to.
     * @param lifeSpan How long this token should be valid for, in seconds.
     * @return A deletion token.
     */
    public DeletionToken createToken(Profile profile, int lifeSpan) {
        if (lifeSpan < MINIMUM_LIFE) {
            throw new IllegalArgumentException("Life too short: " + lifeSpan);
        } else if (lifeSpan > MAXIMUM_LIFE) {
            throw new IllegalArgumentException("Life too long: " + lifeSpan);
        }
        UUID uuid = UUID.randomUUID();
        Date expiry = new Date(System.currentTimeMillis() + (lifeSpan * 1000));
        DeletionToken token = new DeletionToken(profile, uuid, expiry);
        tokens.put(uuid, token);
        return token;
    }

    /**
     * Retrieve a token by its identifier.
     * @param key The identifier of the token.
     * @return the token
     * @throws TokenExpired If the token is too old.
     */
    public DeletionToken retrieveToken(UUID key) throws TokenExpired {
        if (!tokens.containsKey(key)) {
            throw new IllegalArgumentException("No token for " + key);
        }
        DeletionToken token = tokens.get(key);
        Date now = new Date();
        if (now.after(token.getExpiry())) {
            tokens.remove(key);
            throw new TokenExpired();
        }
        return token;
    }

    /**
     * Remove this token.
     * @param token the token to delete.
     */
    public void removeToken(DeletionToken token) {
        tokens.remove(token.getUUID());
    }

    /** Exception indicating a token is too old. **/
    static class TokenExpired extends Exception
    {

        private static final long serialVersionUID = 8392634678344992277L;

    }

}
