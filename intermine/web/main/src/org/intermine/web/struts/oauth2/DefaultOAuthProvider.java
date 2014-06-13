package org.intermine.web.struts.oauth2;

import org.apache.oltu.oauth2.common.OAuthProviderType;

public final class DefaultOAuthProvider implements OAuthProvider {

    private final OAuthProviderType provider;

    public DefaultOAuthProvider(OAuthProviderType provider) {
        this.provider = provider;
    }

    @Override
    public String getTokenUrl() {
        return provider.getTokenEndpoint();
    }

    @Override
    public MessageFormat getMessageFormat() {
        if (OAuthProviderType.MICROSOFT == provider) {
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

}
