package org.intermine.web.logic.profile;

import java.util.HashMap;
import java.util.Map;

import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.profile.Profile;

/**
 * A variant of the bag upgrade runnable that does not record its progress to a
 * session, for use in session-less environments such as the webservices, or
 * potentially in scripting.
 * 
 * @author Alex Kalderimis.
 * 
 */
public class SessionlessBagUpgrade extends UpgradeBagList
{

    public SessionlessBagUpgrade(Profile profile, BagQueryRunner bagQueryRunner) {
        super(profile, bagQueryRunner, null);
    }

    @Override
    protected Map<String, Map<String, Object>> getStatus() {
        return new HashMap<String, Map<String, Object>>();
    }

    protected void reportResult(String name, BagQueryResult result) {
        // NO OP.
    }

}
