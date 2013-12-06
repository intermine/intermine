package org.intermine.webservice.server.user;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.model.userprofile.PermanentToken;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

public class TokensService extends JSONService {

    private final SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final String DENIAL_MSG = "All token requests must be fully authenticated.";

    public TokensService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        Profile profile = getPermission().getProfile();

        UserProfile up = (UserProfile) im.getProfileManager().getProfileObjectStoreWriter().getObjectById(profile.getUserId());
        List<Map<String, Object>> tokens = new ArrayList<Map<String, Object>>();
        for (PermanentToken t: up.getPermanentTokens()) {
            Map<String, Object> token = new HashMap<String, Object>();
            token.put("token", t.getToken());
            token.put("message", t.getMessage());
            token.put("dateCreated", iso8601.format(t.getDateCreated()));
            tokens.add(token);
        }
        this.addResultItem(tokens, false);
    }
    @Override
    protected void validateState() {
        if (!isAuthenticated() || getPermission().isRO()) {
            throw new ServiceForbiddenException(DENIAL_MSG);
        }
    }

    @Override
    protected String getResultsKey() {
        return "tokens";
    }
}
