package org.intermine.web.struts.oauth2;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * A simple record that contains mapping information about the old id of a user.
 * @author Alex Kalderimis
 *
 */
public final class MigrationMapping
{

    private final String oldId, newId;

    /**
     * Construct a mapping for Google OpenID migration.
     * @param openidId The older open-id ID
     * @param sub the newer OpenID Connect (ie. OAuth 2) id.
     **/
    public MigrationMapping(String openidId, String sub) {
        this.oldId = openidId;
        this.newId = sub;
    }

    /**
     * @return the sub
     */
    public String getNewId() {
        return newId;
    }

    /**
     * @return the openid_id
     */
    public String getOldId() {
        return oldId;
    }

    @Override
    public String toString() {
        return "MigrationMapping(" + oldId + " => " + newId + ")";
    }
}
