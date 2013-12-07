package org.intermine.webservice.server.user;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.webservice.server.core.ReadWriteJSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;

public class TokenService extends ReadWriteJSONService {

    public TokenService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        final ProfileManager pm = im.getProfileManager();
        Profile profile = getPermission().getProfile();
        String tokenType = getOptionalParameter("type", "day").toLowerCase();
        String message = getOptionalParameter("message");
        String token = null;
        if ("day".equals(tokenType)) {
            token = pm.generate24hrKey(profile);
        } else if ("once".equals(tokenType)) {
            token = pm.generateSingleUseKey(profile);
        } else if ("api".equals(token)) {
            token = pm.generateApiKey(profile);
        } else if ("perm".equals(tokenType)) {
            token = pm.generateReadOnlyAccessToken(profile, message);
        } else {
            throw new BadRequestException("Unknown token type: " + tokenType);
        }
        addResultValue(token, false);
    }

    @Override
    protected String getResultsKey() {
        return "token";
    }
}
