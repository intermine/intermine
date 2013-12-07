package org.intermine.webservice.server.user;

import java.util.HashMap;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.webservice.server.core.ISO8601DateFormat;
import org.intermine.webservice.server.core.ReadWriteJSONService;

public class NewDeletionTokenService extends ReadWriteJSONService {

    protected final DeletionTokens tokenFactory;

    public NewDeletionTokenService(InterMineAPI im) {
        super(im);
        this.tokenFactory = DeletionTokens.getInstance();
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
        info.put("expiry", ISO8601DateFormat.getFormatter().format(token.getExpiry()));
        info.put("secondsRemaining", (token.getExpiry().getTime() - System.currentTimeMillis()) / 1000);

        this.addResultItem(info, false);
    }

}
