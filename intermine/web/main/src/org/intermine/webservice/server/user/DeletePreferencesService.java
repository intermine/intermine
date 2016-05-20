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

import java.util.Map;

import org.intermine.api.InterMineAPI;
/**
 * A service that deletes a user's preferences.
 * @author Alex Kalderimis
 *
 */
public class DeletePreferencesService extends ReadPreferencesService
{

    /** @param im The InterMine state object. **/
    public DeletePreferencesService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() {
        String key = getOptionalParameter("key");
        Map<String, String> preferences = getPermission().getProfile().getPreferences();
        if (key == null) {
            preferences.clear();
        } else {
            preferences.remove(key);
        }
        super.execute();
    }

}
