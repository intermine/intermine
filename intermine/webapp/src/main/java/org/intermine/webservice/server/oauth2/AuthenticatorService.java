package org.intermine.webservice.server.oauth2;

/*
 * Copyright (C) 2002-2020 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.common.OAuthProviderType;
import org.intermine.api.InterMineAPI;
import org.intermine.web.util.URLUtil;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;

import javax.ws.rs.core.HttpHeaders;
import java.util.*;


/**
 * This service just generates the appropriate link to the oauth2 provider,
 * and redirects the user to that location.
 * Duplication of the Authenticator class used by struts.
 * See org.intermine.web.struts.oauth2.Authenticator
 *
 * @author Alex Kalderimis, Daniela Butano
 *
 */
public class AuthenticatorService extends JSONService
{
    private static final Logger LOG = Logger.getLogger(AuthenticatorService.class);

    public AuthenticatorService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        String providerName = getRequiredParameter("provider");
        String redirectUri = getRedirectUri(webProperties, providerName);
        String realm = webProperties.getProperty("webapp.baseurl");
        String state = UUID.randomUUID().toString();
        //TODO there is no session on the ws
        //request.getSession().setAttribute("oauth2.state", state);

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
                    .setRedirectURI(redirectUri)
                    .setScope(webProperties.getProperty("oauth2." + providerName + ".scopes"))
                    .setState(state)
                    .setParameter("response_type", "code")
                    .setParameter("openid.realm", realm) // link open-id 2.0 accounts [1]
                    .buildQueryMessage();
        String goHere = authRequest.getLocationUri();
        addResultEntry("link", goHere,false);
    }

    private String getRedirectUri(Properties webProperties, String providerName) {
        List<String> redirectParts = new ArrayList<String>();
        redirectParts.add(webProperties.getProperty("webapp.baseurl"));
        redirectParts.add(webProperties.getProperty("webapp.path"));
        redirectParts.add("service/oauth2callback?provider=" + providerName);
        String redirectUri = StringUtils.join(redirectParts, "/");
        return redirectUri;
    }
}
