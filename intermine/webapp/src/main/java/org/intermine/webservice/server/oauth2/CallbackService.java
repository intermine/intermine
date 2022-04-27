package org.intermine.webservice.server.oauth2;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.GitHubTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.OAuthProviderType;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.DuplicateMappingException;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.UserPreferences;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.profile.LoginHandler;
import org.intermine.web.logic.profile.ProfileMergeIssues;
import org.intermine.web.struts.oauth2.OAuthProvider;
import org.intermine.web.struts.oauth2.DelegatedIdentity;
import org.intermine.web.struts.oauth2.ForseenProblem;
import org.intermine.web.struts.oauth2.CustomOAuthProvider;
import org.intermine.web.struts.oauth2.DefaultOAuthProvider;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.user.JSONUserFormatter;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * The service handles the requests made after the user has visited their
 * authorisation provider to log-in. That provider will then send them off here with
 * a code that we need to use to access their user details via a two-step process
 * (get an authorisation token, and then get user details). If all is well, this
 * controller will log the user in
 * Duplication of the Callback class used by struts.
 * See org.intermine.web.struts.oauth2.Callback
 *
 * @author Alex Kalderimis, Daniela Butano
 *
 */
public class CallbackService extends JSONService
{
    private static final Logger LOG = Logger.getLogger(CallbackService.class);

    /**
     * Constructor
     * @param im The InterMine state object.
     **/
    public CallbackService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected String getResultsKey() {
        return "output";
    }

    @Override
    protected void execute() throws Exception {
        String providerName = getRequiredParameter("provider");
        String redirectUri = getRequiredParameter("redirect_uri");

        try {
            OAuthProvider provider = getOAuthProvider(webProperties, providerName);
            OAuthAuthzResponse oar = getAuthResponse(request);

            // Step one - get token
            String accessToken = getAccessToken(redirectUri, oar, provider);
            // Step two - exchange token for identity
            DelegatedIdentity identity = getDelegatedIdentity(providerName, accessToken);

            Profile profile = getProfile(identity);
            Map<String, Object> output = new HashMap<String, Object>();
            JSONUserFormatter formatter = new JSONUserFormatter(profile);
            output.put("user", new JSONObject(formatter.format()));
            output.put("token", im.getProfileManager().generate24hrKey(profile));

            //merge anonymous profile with the logged profile
            Profile currentProfile = getPermission().getProfile();
            ProfileMergeIssues issues = null;
            if (currentProfile != null && StringUtils.isEmpty(currentProfile.getUsername())) {
                // The current profile was for an anonymous guest.
                issues = LoginHandler.mergeProfiles(currentProfile, profile);
                output.put("renamedLists", new JSONObject(issues.getRenamedBags()));
            } else {
                output.put("renamedLists", new JSONObject(Collections.emptyMap()));
            }

            addResultItem(output, false);

        } catch (Exception e) {
            LOG.error("Error granting access", e);
        }
    }

    private OAuthProvider getOAuthProvider(
            Properties webProperties,
            String providerName) throws ForseenProblem {
        OAuthProvider provider;
        try {
            provider = getProvider(webProperties, providerName);
        } catch (IllegalArgumentException e) {
            throw new ForseenProblem("oauth2.error.unknown-provider", providerName);
        }
        return provider;
    }

    private OAuthProvider getProvider(Properties properties, String providerName) {
        if (properties.containsKey("oauth2." + providerName + ".url.token")) {
            // Presence of this key implies all other options.
            return new CustomOAuthProvider(properties, providerName);
        } else {
            return new DefaultOAuthProvider(properties, OAuthProviderType.valueOf(providerName));
        }
    }

    private OAuthAuthzResponse getAuthResponse(HttpServletRequest request)
            throws ForseenProblem {
        OAuthAuthzResponse oar;
        try {
            oar = OAuthAuthzResponse.oauthCodeAuthzResponse(request);
        } catch (OAuthProblemException e) {
            throw new ForseenProblem("oauth2.error.getting-code", e.getMessage());
        }
        return oar;
    }

    private String getAccessToken(String redirect, OAuthAuthzResponse oar,
                                  OAuthProvider provider)
            throws OAuthSystemException, OAuthProblemException {
        OAuthClient oauthClient = new OAuthClient(new URLConnectionClient());
        OAuthClientRequest clientReq;
        OAuthClientRequest.TokenRequestBuilder requestBuilder = OAuthClientRequest
                .tokenLocation(provider.getTokenUrl())
                .setGrantType(GrantType.AUTHORIZATION_CODE)
                .setClientId(provider.getClientId())
                .setClientSecret(provider.getClientSecret())
                .setRedirectURI(redirect)
                .setCode(oar.getCode());

        switch (provider.getMessageFormat()) {
            case BODY:
                clientReq = requestBuilder.buildBodyMessage();
                break;
            case QUERY:
                clientReq = requestBuilder.buildQueryMessage();
                break;
            default:
                throw new RuntimeException("Unknown message format");
        }
        LOG.info("Requesting access token: URI = " + clientReq.getLocationUri()
                + " BODY = " + clientReq.getBody());

        try {
            OAuthAccessTokenResponse tokenResponse = null;
            switch (provider.getResponseType()) {
                case FORM:
                    tokenResponse = oauthClient.accessToken(clientReq, GitHubTokenResponse.class);
                    break;
                case JSON:
                    tokenResponse = oauthClient.accessToken(clientReq);
                    break;
                default:
                    throw new RuntimeException("Unknown response type");
            }
            return tokenResponse.getAccessToken();
        } catch (OAuthProblemException ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }

    private DelegatedIdentity getDelegatedIdentity(String providerName, String accessToken)
            throws OAuthSystemException, OAuthProblemException, JSONException {
        if (providerIsSane(providerName)) {
            return getSaneProviderUserInfo(providerName, accessToken);
        }
        throw new RuntimeException("Missing config: "
                + "oauth2." + providerName + ".identity-resource");
    }

    private boolean providerIsSane(String providerName) {
        Properties webProperties = InterMineContext.getWebProperties();
        return webProperties.containsKey("oauth2." + providerName + ".identity-resource");
    }

    /**
     * Get user info for services which are sane enough to have an identity resource
     * that serves json
     * with <code>id</code>, <code>email</code> and <code>name</code> keys.
     * @param provider Who to ask.
     * @param accessToken An access token.
     * @return The delegated identity.
     * @throws OAuthSystemException
     * @throws OAuthProblemException
     * @throws JSONException If things aren't so sane after all.
     */
    private DelegatedIdentity getSaneProviderUserInfo(String provider, String accessToken)
            throws OAuthSystemException, OAuthProblemException, JSONException {
        Properties props = InterMineContext.getWebProperties();
        String prefix = "oauth2." + provider;

        String identityEndpoint = props.getProperty(prefix + ".identity-resource");
        String envelopeKey = props.getProperty(prefix + ".identity-envelope");
        String idKey = props.getProperty(prefix + ".id-key", "id");
        String nameKey = props.getProperty(prefix + ".name-key", "name");
        String emailKey = props.getProperty(prefix + ".email-key", "email");
        String authMechanism = props.getProperty(prefix + ".resource-auth-mechanism", "queryparam");

        OAuthBearerClientRequest requestBuilder =
                new OAuthBearerClientRequest(identityEndpoint).setAccessToken(accessToken);

        OAuthClientRequest bearerClientRequest;
        if ("queryparam".equals(authMechanism)) {
            bearerClientRequest = requestBuilder.buildQueryMessage();
        } else if ("header".equals(authMechanism)) {
            bearerClientRequest = requestBuilder.buildHeaderMessage();
        } else if ("body".equals(authMechanism)) {
            bearerClientRequest = requestBuilder.buildBodyMessage();
        } else {
            throw new OAuthSystemException("Unknown authorisation mechanism: " + authMechanism);
        }
        LOG.debug("Requesting identity information:"
                + " URI = " + bearerClientRequest.getLocationUri()
                + " HEADERS = " + bearerClientRequest.getHeaders()
                + " BODY = " + bearerClientRequest.getBody());

        bearerClientRequest.setHeader("Accept", "application/json");
        OAuthClient oauthClient = new OAuthClient(new URLConnectionClient());
        OAuthResourceResponse resp = oauthClient.resource(bearerClientRequest,
                OAuth.HttpMethod.GET, OAuthResourceResponse.class);

        return parseIdentity(provider, envelopeKey, idKey, nameKey, emailKey, resp.getBody());
    }

    private DelegatedIdentity parseIdentity(String provider,
                                            String envelopeKey, String idKey, String nameKey,
                                            String emailKey, String body) throws JSONException {
        JSONObject result = new JSONObject(body);
        if (StringUtils.isNotBlank(envelopeKey)) {
            result = result.getJSONObject(envelopeKey);
        }
        String id = result.get(idKey).toString();
        String[] nameKeyParts = nameKey.split(",");
        String[] nameParts = new String[nameKeyParts.length];
        for (int i = 0; i < nameKeyParts.length; i++) {
            nameParts[i] = result.getString(nameKeyParts[i]);
        }
        String name = StringUtils.join(nameParts, " ");
        String email;
        // either {emails: {preferred}} or {email}
        JSONObject emails = result.optJSONObject("emails");
        if (emails == null) {
            email = result.optString(emailKey);
        } else {
            email = emails.optString("preferred");
            if (email == null) {
                email = emails.optString("account");
            }
        }
        return new DelegatedIdentity(provider, id, email, name);
    }

    private Profile getProfile(DelegatedIdentity identity) {
        Profile profile = im.getProfileManager()
                .grantPermission(identity.getProvider(), identity.getId(), im.getClassKeys())
                .getProfile();
        Map<String, String> preferences = profile.getPreferences();
        if (!preferences.containsKey(UserPreferences.EMAIL)) {
            preferences.put(UserPreferences.EMAIL, identity.getEmail());
        }

        // Always write a user's email address to the user profile. This is unlikely to change
        // but fixes problems associated with Google returning blank strings in the oAuth2 profile.
        preferences.put(UserPreferences.EMAIL, identity.getEmail());

        // If we have a non empty identity from the provider
        String identityName = identity.getName();
        if (!("".equals(identityName))) {
            // ...and an AKA preference exists and it's empty,
            // or if the key is missing (never set), then populate it
            String aka = "";

            if (preferences.containsKey(UserPreferences.AKA)) {
                aka = preferences.get(UserPreferences.AKA);
            }

            if ("".equals(aka)) {
                preferences.put(UserPreferences.AKA, identityName);
            }

            // ...and an ALIAS preference exists and it's empty,
            // or if the key is missing (never set), then populate it
            String alias = "";

            if (preferences.containsKey(UserPreferences.ALIAS)) {
                alias = preferences.get(UserPreferences.ALIAS);
            }

            if ("".equals(alias)) {
                int c = 0;
                alias = identityName;

                do {
                    try {
                        preferences.put(UserPreferences.ALIAS, alias);
                    } catch (DuplicateMappingException e) {
                        alias = identityName + " " + ++c;
                    }
                } while (!preferences.containsKey(UserPreferences.ALIAS));
            }
        }

        return profile;
    }
}
