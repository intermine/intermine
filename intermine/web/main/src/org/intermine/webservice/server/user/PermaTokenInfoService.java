package org.intermine.webservice.server.user;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;

import org.intermine.api.InterMineAPI;
import org.intermine.model.userprofile.PermanentToken;
import org.intermine.webservice.server.core.ReadWriteJSONService;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;

/**
 * @author Alex Kalderimis
 */
public class PermaTokenInfoService extends ReadWriteJSONService
{

    private String uuid;

    /**
     * @param im The InterMine state object.
     * @param uuid The token we want to know more about.
     */
    public PermaTokenInfoService(InterMineAPI im, String uuid) {
        super(im);
        this.uuid = uuid;
    }

    @Override
    protected void execute() throws Exception {
        PermanentToken token = new PermanentToken();
        token.setToken(uuid);
        token = (PermanentToken) im.getProfileManager()
                  .getProfileObjectStoreWriter()
                  .getObjectByExample(token, Collections.singleton("token"));
        if (token == null) {
            throw new ResourceNotFoundException(uuid + " is not a token");
        }
        addResultItem(PermaTokens.format(token), false);
    }

    @Override
    public String getResultsKey() {
        return "token";
    }

}
