package org.intermine.webservice.server.oauth2;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.common.OAuthProviderType;
import org.intermine.api.InterMineAPI;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;

/**
 * This service just generates the appropriate link to the oauth2 provider
 * Similar to the Authenticator class used by struts.
 * See org.intermine.web.struts.oauth2.Authenticator
 *
 * @author Daniela Butano
 *
 */
public class AuthenticatorService extends JSONService
{
    private static final Logger LOG = Logger.getLogger(AuthenticatorService.class);

    /**
     * Constructor
     * @param im The InterMine state object.
     **/
    public AuthenticatorService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        String providerName = getRequiredParameter("provider");
        String realm = webProperties.getProperty("webapp.baseurl");

        String authorisationUrl = webProperties.getProperty("oauth2." + providerName + ".url.auth");
        if (authorisationUrl == null) {
            try {
                OAuthProviderType providerType = OAuthProviderType.valueOf(providerName);
                authorisationUrl = providerType.getAuthzEndpoint();
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Provider name " + providerName + " unknown");
            }
        }
        OAuthClientRequest authRequest = OAuthClientRequest
                .authorizationLocation(authorisationUrl)
                .setClientId(webProperties.getProperty("oauth2." + providerName + ".client-id"))
                .setScope(webProperties.getProperty("oauth2." + providerName + ".scopes"))
                .setParameter("response_type", "code")
                .setParameter("openid.realm", realm) // link open-id 2.0 accounts [1]
                .buildQueryMessage();
        String link = authRequest.getLocationUri();
        addResultEntry("link", link, false);
    }
}
