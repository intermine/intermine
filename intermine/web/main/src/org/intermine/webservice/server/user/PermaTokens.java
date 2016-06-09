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

import java.util.HashMap;
import java.util.Map;

import org.intermine.model.userprofile.PermanentToken;
import org.intermine.webservice.server.core.ISO8601DateFormat;

/** @author Alex Kalderimis **/
public final class PermaTokens
{

    private PermaTokens() {
        // hidden.
    }

    /**
     * Format a permanent token as a map.
     * @param token The token
     * @return a map of properties.
     */
    public static Map<String, Object> format(PermanentToken token) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("token", token.getToken());
        map.put("message", token.getMessage());
        map.put("dateCreated", ISO8601DateFormat.getFormatter().format(token.getDateCreated()));
        return map;
    }
}
