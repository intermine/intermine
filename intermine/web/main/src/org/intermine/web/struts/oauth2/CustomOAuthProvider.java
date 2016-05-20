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

/**
 * A representation of an OAuth provider which has been configured
 * entirely in web-properties.
 *
 * @author Alex Kalderimis
 *
 */
public final class CustomOAuthProvider implements OAuthProvider
{

    private final String name, tokenUrl, clientId, clientSecret;
    private final MessageFormat messageFormat;
    private final ResponseType responseType;

    /**
     * Construct a new provider.
     * @param properties The properties which define the provider.
     * @param providerName The name of the provider to look up the configured values.
     */
    public CustomOAuthProvider(Properties properties, String providerName) {
        String prefix = "oauth2." + providerName;
        name = providerName;
        tokenUrl = properties.getProperty(prefix + ".url.token");
        clientId = properties.getProperty(prefix + ".client-id");
        clientSecret = properties.getProperty(prefix + ".client-secret");
        messageFormat = MessageFormat.valueOf(properties.getProperty(prefix + ".messageformat"));
        responseType = ResponseType.valueOf(properties.getProperty(prefix + ".responsetype"));
        if (tokenUrl == null) {
            throw new IllegalArgumentException("Bad config: tokenUrl must not be null.");
        }
        if (StringUtils.isBlank(clientId) || StringUtils.isBlank(clientSecret)) {
            throw new IllegalArgumentException("Bad config: no client config for " + providerName);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public String getClientSecret() {
        return clientSecret;
    }

    @Override
    public String getTokenUrl() {
        return tokenUrl;
    }

    @Override
    public MessageFormat getMessageFormat() {
        return messageFormat;
    }

    @Override
    public ResponseType getResponseType() {
        return responseType;
    }

}
