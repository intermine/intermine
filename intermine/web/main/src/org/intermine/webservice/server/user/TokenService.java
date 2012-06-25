package org.intermine.webservice.server.user;

import java.util.Arrays;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;
import org.intermine.webservice.server.output.JSONFormatter;

public class TokenService extends JSONService {


    private static final String DENIAL_MSG = "All token requests must be authenticated.";

    public TokenService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        final ProfileManager pm = im.getProfileManager();
        Profile profile = getPermission().getProfile();

        String token = pm.generate24hrKey(profile);
        output.addResultItem(Arrays.asList("\"" + token + "\""));
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
        retval.put(JSONFormatter.KEY_INTRO, "\"token\":");
        return retval;
    }

}
