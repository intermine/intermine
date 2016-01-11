package org.intermine.web.struts.oauth2;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.oltu.oauth2.common.OAuthProviderType;

/**
 * A representation of an OAuth provider which is one of the default
 * providers provided for end-users.
 *
 * This list of currently supplied providers currently includes those
 * supplied with the OLTU library.
 *
 * @author Alex Kalderimis
 *
 */
public final class DefaultOAuthProvider implements OAuthProvider
{

    private final String clientId, clientSecret;
    private final OAuthProviderType provider;

    /**
     * Construct a new provider.
     *
     * @param properties The properties where the client-id and client-secret can be looked up.
     * @param provider The OLTU provider configuration.
     */
    public DefaultOAuthProvider(Properties properties, OAuthProviderType provider) {
        this.provider = provider;
        String prefix = "oauth2." + provider.name();
        clientId = properties.getProperty(prefix + ".client-id");
        clientSecret = properties.getProperty(prefix + ".client-secret");
        if (StringUtils.isBlank(clientId) || StringUtils.isBlank(clientSecret)) {
            throw new IllegalArgumentException("Bad config: no client config for " + provider);
        }
    }

    @Override
    public String getTokenUrl() {
        if (OAuthProviderType.GOOGLE == provider) {
            // Use the newer token endpoint.
            return "https://www.googleapis.com/oauth2/v3/token";
        }
        return provider.getTokenEndpoint();
    }

    @Override
    public MessageFormat getMessageFormat() {
        if (OAuthProviderType.MICROSOFT == provider
                || OAuthProviderType.GOOGLE == provider) {
            return MessageFormat.BODY;
        }
        return MessageFormat.QUERY;
    }

    @Override
    public ResponseType getResponseType() {
        if (OAuthProviderType.GITHUB == provider
                || OAuthProviderType.FACEBOOK == provider) {
            return ResponseType.FORM;
        }
        return ResponseType.JSON;
    }

    @Override
    public String getName() {
        return provider.name();
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public String getClientSecret() {
        return clientSecret;
    }

}
