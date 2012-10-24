package org.intermine.webservice.server.user;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;
import org.json.JSONObject;

/**
 * Class for retrieving information about the currently authenticated user.
 *
 * Currently just a stub. This will be fleshed out in future with any useful user data:
 * <ul>
 *   <li>preferences (enrichment algorithms, preferred extra-values).</li>
 *   <li>roles and groups</li>
 *   <li>etc...</li>
 * </ul>
 * @author Alex Kalderimis
 *
 */
public class WhoAmIService extends JSONService
{

    private static final String DENIAL_MSG = "All whoami requests must be authenticated.";

    /**
     * Constructor
     * @param im The InterMine API object.
     */
    public WhoAmIService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void validateState() {
        if (!isAuthenticated()) {
            throw new ServiceForbiddenException(DENIAL_MSG);
        }
    }

    @Override
    protected String getResultsKey() {
        return "user";
    }

    @Override
    protected void execute() throws Exception {
        Profile profile = getPermission().getProfile();
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("username", profile.getUsername());
        data.put("preferences", profile.getPreferences());
        addResultItem(data, false);
    }


}
