package org.intermine.webservice.server.user;

/*
 * Copyright (C) 2002-2019 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.api.profile.Profile;

import java.util.HashMap;
import java.util.Map;

public class JSONUserFormatter
{
    private final Profile profile;

    public JSONUserFormatter(Profile profile) {
        this.profile = profile;
    }

    public Map<String, Object> format() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("username", profile.getUsername());
        String id = (profile.getUserId() != null) ? profile.getUserId().toString() : null;
        data.put("id", id);
        data.put("preferences", profile.getPreferences());
        data.put("superuser", profile.isSuperuser());
        return data;
    }
}
