package org.intermine.webservice.server.user;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;
import org.intermine.webservice.server.output.JSONFormatter;
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
    protected Map<String, Object> getHeaderAttributes() {
        Map<String, Object> retval = super.getHeaderAttributes();
        retval.put(JSONFormatter.KEY_INTRO, "\"user\":");
        return retval;
    }

    @Override
    protected void execute() throws Exception {
        Profile profile = SessionMethods.getProfile(request.getSession());
        JSONObject user = new JSONObject();
        user.put("username", profile.getUsername());

        output.addResultItem(Arrays.asList(user.toString()));
    }


}
