package org.intermine.web.struts.oauth2;

import java.util.Properties;

public final class CustomOAuthProvider implements OAuthProvider {

    private final String tokenUrl;
    private final MessageFormat messageFormat;
    private final ResponseType responseType;

    public CustomOAuthProvider(Properties properties, String providerName) {
        tokenUrl = properties.getProperty("oauth2." + providerName + ".url.token");
        messageFormat = MessageFormat.valueOf(properties.getProperty("oauth2." + providerName + ".messageformat"));
        responseType = ResponseType.valueOf(properties.getProperty("oauth2." + providerName + ".responsetype"));
        if (tokenUrl == null) {
            throw new IllegalArgumentException("tokenUrl must not be null");
        }
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
