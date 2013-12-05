package org.intermine.webservice.server.user;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

public class NewDeletionTokenService extends JSONService {

    private static final String DENIAL_MSG
        = "All requests for account deletion tokens must be authenticated";
    protected final DeletionTokens tokenFactory;
    private final SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    public NewDeletionTokenService(InterMineAPI im) {
        super(im);
        this.tokenFactory = DeletionTokens.getInstance();
    }

    @Override
    protected void validateState() {
        if (!isAuthenticated()) {
            throw new ServiceForbiddenException(DENIAL_MSG);
        }
    }

    @Override
    protected String getResultsKey() {
        return "token";
    }

    @Override
    protected void execute() throws Exception {
        int lifeSpan = getIntParameter("validity", 60);
        Profile profile = getPermission().getProfile();
        DeletionToken token = tokenFactory.createToken(profile, lifeSpan);
        serveToken(token);
    }

    protected void serveToken(DeletionToken token) {
        Map<String, Object> info = new HashMap<String, Object>();

        info.put("uuid", token.getUUID().toString());
        info.put("expiry", iso8601.format(token.getExpiry()));
        info.put("secondsRemaining", (token.getExpiry().getTime() - System.currentTimeMillis()) / 1000);

        this.addResultItem(info, false);
    }

}
