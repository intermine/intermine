package org.intermine.webservice.server.user;

import org.intermine.api.InterMineAPI;

public class DeletionTokenCancellationService extends DeletionTokenInfoService {

    public DeletionTokenCancellationService(InterMineAPI im, String uuid) {
        super(im, uuid);
    }

    @Override
    protected void execute() {
        DeletionToken token = getToken();
        tokenFactory.removeToken(token);
    }

}
