package org.intermine.webservice.server;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.webservice.server.core.JSONService;

/**
 *  Open a new 24-hour session.
 *  
 *  For authenticated users this just issues a new 24hr token,
 *  but for unauthenticated users it assigns a new in-memory profile,
 *  allowing users to save lists
 *  and all that good authenticated stuff, without actually creating a user.
 * 
 * @author Alex Kalderimis
 *
 */
public class SessionService extends JSONService {

    public SessionService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected String getResultsKey() {
        return "token";
    }

    @Override
    protected void execute() throws Exception {
        ProfileManager pm = im.getProfileManager();
        Profile p;
        if (isAuthenticated()) {
            p = getPermission().getProfile();
        } else {
            p = pm.createAnonymousProfile();
            p.disableSaving();
        }
        String token = pm.generate24hrKey(p);
        addResultValue(token, false);
    }

}
