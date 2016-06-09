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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest.TokenRequestBuilder;
import org.apache.oltu.oauth2.client.response.GitHubTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.OAuthProviderType;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.DuplicateMappingException;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.UserPreferences;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.profile.LoginHandler;
import org.intermine.web.logic.profile.ProfileMergeIssues;
import org.intermine.web.logic.session.SessionMethods;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The controller that handles the requests made after the user has visited their
 * authorisation provider to log-in. That provider will then send them off here with
 * a code that we need to use to access their user details via a two-step process
 * (get an authorisation token, and then get user details). If all is well, this
 * controller will log the user in, and forward the request on to the mymine page.
 *
 * @author Alex Kalderimis
 *
 */
public class Callback extends LoginHandler
{

    private static final Logger LOG = Logger.getLogger(Callback.class);

    /**
     * Method called for login in
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     */
    @Override public ActionForward execute(
            ActionMapping mapping,
            ActionForm form,
            HttpServletRequest request,
            HttpServletResponse response) {
        Properties webProperties = InterMineContext.getWebProperties();

        // Suitable values are: GOOGLE, GITHUB, FACEBOOK, MICROSOFT, etc.
        String providerName = request.getParameter("provider");
        String redirectUri = getRedirectUri(webProperties, providerName);

        try {
            OAuthProvider provider = getOAuthProvider(mapping, request,
                    webProperties, providerName);
            OAuthAuthzResponse oar = getAuthResponse(mapping, request);
            checkOauthState(mapping, request, oar);

            ActionMessages messages = null;
            if ("GOOGLE".equals(providerName)) {
                messages = googleProviderFlow(request, providerName, redirectUri, provider, oar);
            } else {
                messages = standardProviderFlow(request, providerName, redirectUri, provider, oar);
            }
            saveMessages(request, messages);
            return mapping.findForward("mymine");
        } catch (ForseenProblem e) {
            ActionErrors errors = new ActionErrors();
            errors.add(ActionErrors.GLOBAL_MESSAGE, e.getActionMessage());
            saveErrors(request, errors);
            return mapping.findForward("login");
        } catch (Exception e) {
            LOG.error("Error granting access", e);
            ActionErrors errors = new ActionErrors();
            errors.add(ActionErrors.GLOBAL_MESSAGE,
                    new ActionMessage("oauth2.error.granting", e.getLocalizedMessage()));
            saveErrors(request, errors);
            return mapping.findForward("login");
        }
    }

    /**
     * Google's OpenID2.0 -> OpenIDConnect (ie. Open Auth 2.0) migration makes this
     * special branch necessary.
     */
    private ActionMessages googleProviderFlow(HttpServletRequest request,
            String providerName, String redirectUri, OAuthProvider provider,
            OAuthAuthzResponse oar)
        throws ForseenProblem, OAuthSystemException, OAuthProblemException, JSONException {
        // Special flow just for Google, because Google is special (not in a good way).
        OAuthAccessTokenResponse resp = getTokenResponse(redirectUri, oar, provider);
        LOG.debug("GOOGLE RESPONSE: " + resp.getBody());

        MigrationMapping migrationMapping = null;
        Base64 decoder = new Base64();
        String accessToken = resp.getAccessToken();
        JSONObject respData;
        try {
            respData = new JSONObject(resp.getBody());
        } catch (JSONException e) {
            throw new ForseenProblem("oauth2.error.bad-json");
        }
        String jwt = respData.optString("id_token");
        if (jwt != null) {
            String[] pieces = jwt.split("\\.");
            if (pieces.length == 3) {
                JSONObject claims = new JSONObject(new String(decoder.decode(pieces[1])));
                String openidID = claims.optString("openid_id");
                String sub = claims.optString("sub");
                migrationMapping = new MigrationMapping(openidID, sub);
            } else {
                LOG.error("id_token is not a valid JWT - has Google changed their API?");
            }
        } else {
            LOG.debug("No id_token (and thus migration info) provided by Google");
        }
        DelegatedIdentity identity = getDelegatedIdentity(providerName, accessToken);
        return loginUser(request, identity, migrationMapping);
    }

    private ActionMessages standardProviderFlow(HttpServletRequest request,
            String providerName, String redirectUri, OAuthProvider provider,
            OAuthAuthzResponse oar) throws OAuthSystemException,
            OAuthProblemException, JSONException {
        ActionMessages messages;
        // Step one - get token
        String accessToken = getAccessToken(redirectUri, oar, provider);
        // Step two - exchange token for identity
        DelegatedIdentity identity = getDelegatedIdentity(providerName, accessToken);
        // Step three - huzzah! Inform user of who they are.
        messages = loginUser(request, identity);
        return messages;
    }

    private String getRedirectUri(Properties webProperties,
            String providerName) {
        List<String> redirectParts = new ArrayList<String>();
        redirectParts.add(webProperties.getProperty("webapp.baseurl"));
        redirectParts.add(webProperties.getProperty("webapp.path"));
        redirectParts.add("oauth2callback.do?provider=" + providerName);
        return StringUtils.join(redirectParts, "/");
    }

    private OAuthAuthzResponse getAuthResponse(ActionMapping mapping,
            HttpServletRequest request) throws ForseenProblem {
        OAuthAuthzResponse oar;
        try {
            oar = OAuthAuthzResponse.oauthCodeAuthzResponse(request);
        } catch (OAuthProblemException e) {
            throw new ForseenProblem("oauth2.error.getting-code", e.getMessage());
        }
        return oar;
    }

    private void checkOauthState(ActionMapping mapping,
            HttpServletRequest request, OAuthAuthzResponse oar) throws ForseenProblem {
        String state = (String) request.getSession().getAttribute("oauth2.state");
        if (state == null || !state.equals(oar.getState())) {
            throw new ForseenProblem("oauth2.error.illegal-request");
        }
    }

    private OAuthProvider getOAuthProvider(
            ActionMapping mapping,
            HttpServletRequest request,
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

    private OAuthAccessTokenResponse
    getTokenResponse(String redirect, OAuthAuthzResponse oar, OAuthProvider provider)
        throws OAuthSystemException, OAuthProblemException {
        OAuthClient oauthClient = new OAuthClient(new URLConnectionClient());
        OAuthClientRequest clientReq;
        TokenRequestBuilder requestBuilder = OAuthClientRequest
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

        switch (provider.getResponseType()) {
            case FORM:
                return oauthClient.accessToken(clientReq, GitHubTokenResponse.class);
            case JSON:
                return oauthClient.accessToken(clientReq);
            default:
                throw new RuntimeException("Unknown response type");
        }
    }

    private String getAccessToken(String redirect, OAuthAuthzResponse oar, OAuthProvider provider)
        throws OAuthSystemException, OAuthProblemException {
        OAuthAccessTokenResponse oauthResponse = getTokenResponse(redirect, oar, provider);
        String accessToken = oauthResponse.getAccessToken();
        return accessToken;
    }

    private OAuthProvider getProvider(Properties properties, String providerName) {
        if (properties.containsKey("oauth2." + providerName + ".url.token")) {
            // Presence of this key implies all other options.
            return new CustomOAuthProvider(properties, providerName);
        } else {
            return new DefaultOAuthProvider(properties, OAuthProviderType.valueOf(providerName));
        }
    }

    private ActionMessages loginUser(
            HttpServletRequest request,
            DelegatedIdentity identity) {
        return loginUser(request, identity, null);
    }

    private ActionMessages loginUser(
            HttpServletRequest request,
            DelegatedIdentity identity,
            MigrationMapping mapping) {
        LOG.debug("Logging in " + identity + " with migration mapping " + mapping);
        Profile currentProfile = SessionMethods.getProfile(request.getSession());
        InterMineAPI api = SessionMethods.getInterMineAPI(request.getSession());
        Profile profile = api.getProfileManager()
                 .grantPermission(identity.getProvider(), identity.getId(), api.getClassKeys())
                 .getProfile();
        Map<String, String> preferences = profile.getPreferences();
        if (!preferences.containsKey(UserPreferences.EMAIL)) {
            preferences.put(UserPreferences.EMAIL, identity.getEmail());
        }
        if (!preferences.containsKey(UserPreferences.AKA)) {
            preferences.put(UserPreferences.AKA, identity.getName());
        }
        if (!preferences.containsKey(UserPreferences.ALIAS)) {
            int c = 0;
            String alias = identity.getName();
            while (!preferences.containsKey(UserPreferences.ALIAS)) {
                try {
                    preferences.put(UserPreferences.ALIAS, alias);
                } catch (DuplicateMappingException e) {
                    alias = identity.getName() + " " + ++c;
                }
            }
        }
        ActionMessages messages = new ActionMessages();
        setUpProfile(request.getSession(), profile);
        messages.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage(
                "login.oauth2.successful", identity.getProvider(), profile.getName()));
        ProfileMergeIssues issues = new ProfileMergeIssues();

        if (currentProfile != null && StringUtils.isEmpty(currentProfile.getUsername())) {
            // The current profile was for an anonymous guest.
            issues = mergeProfiles(currentProfile, profile);
        }
        if (mapping != null) {
            Profile migratedFrom = api.getProfileManager().getProfile(mapping.getOldId());
            if (migratedFrom != null) {
                issues = issues.combineWith(mergeProfiles(migratedFrom, profile));
                profile.setApiKey(migratedFrom.getApiKey());
                Map<String, String> prefs =
                        new HashMap<String, String>(migratedFrom.getPreferences());
                migratedFrom.getPreferences().clear();
                profile.getPreferences().putAll(prefs);
                UserProfile oldUser = api.getProfileManager()
                                         .getUserProfile(migratedFrom.getUserId());
                if (oldUser != null) { // mark old profile as migrated.
                    oldUser.setUsername("__migrated__" + oldUser.getUsername());
                    try {
                        api.getUserProfile().store(oldUser);
                    } catch (ObjectStoreException e) {
                        messages.add(ActionMessages.GLOBAL_MESSAGE,
                                new ActionMessage("login.migration.error",
                                        mapping.getOldId(), e.getMessage()));
                    }
                }
            }
        }
        for (Entry<String, String> pair: issues.getRenamedBags().entrySet()) {
            messages.add(ActionMessages.GLOBAL_MESSAGE,
                    new ActionMessage("login.renamed.bag", pair.getKey(), pair.getValue()));
        }
        for (Map.Entry<String, String> renamed: issues.getRenamedTemplates().entrySet()) {
            messages.add(ActionMessages.GLOBAL_MESSAGE,
                new ActionMessage("login.failedtemplate", renamed.getKey(), renamed.getValue()));
        }

        return messages;
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
            String envelopeKey, String idKey, String nameKey, String emailKey,
            String body) throws JSONException {
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
}
