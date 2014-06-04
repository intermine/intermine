package org.intermine.web.struts.oauth2;

public interface OAuthProvider {

    String getTokenUrl();

    MessageFormat getMessageFormat();

    ResponseType getResponseType();
}
