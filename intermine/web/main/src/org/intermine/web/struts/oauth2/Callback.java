package org.intermine.web.struts.oauth2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.UserPreferences;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.profile.LoginHandler;
import org.intermine.web.logic.session.SessionMethods;
import org.json.JSONException;
import org.json.JSONObject;

public class Callback extends LoginHandler {

    private static final Logger LOG = Logger.getLogger(Callback.class);

    /**
     * Method called for login in
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws an exception
     */
    @Override public ActionForward execute(
            ActionMapping mapping,
            ActionForm form,
            HttpServletRequest request,
            HttpServletResponse response) {
        Properties webProperties = InterMineContext.getWebProperties();
        
        // Suitable values are: GOOGLE, GITHUB, FACEBOOK, MICROSOFT, etc.
        String providerName = request.getParameter("provider");

        List<String> redirectParts = new ArrayList<String>();
        redirectParts.add(webProperties.getProperty("webapp.baseurl"));
        redirectParts.add(webProperties.getProperty("webapp.path"));
        redirectParts.add("oauth2callback.do?provider=" + providerName);

        OAuthAuthzResponse oar;
        try {
            oar = OAuthAuthzResponse.oauthCodeAuthzResponse(request);
        } catch (OAuthProblemException e) {
            ActionErrors errors = new ActionErrors();
            errors.add(ActionErrors.GLOBAL_MESSAGE, new ActionMessage("oauth2.error.getting-code", e.getMessage()));
            saveErrors(request, errors);
            return mapping.findForward("login");
        }

        String state = (String) request.getSession().getAttribute("oauth2.state");
        if (state == null || !state.equals(oar.getState())) {
            ActionErrors errors = new ActionErrors();
            errors.add(ActionErrors.GLOBAL_MESSAGE, new ActionMessage("oauth2.error.illegal-request"));
            saveErrors(request, errors);
            return mapping.findForward("login");
        }

        OAuthProvider provider;
        try {
            provider = getProvider(webProperties, providerName);
        } catch (IllegalArgumentException e) {
            ActionErrors errors = new ActionErrors();
            errors.add(ActionErrors.GLOBAL_MESSAGE, new ActionMessage("oauth2.error.unknown-provider", providerName));
            saveErrors(request, errors);
            return mapping.findForward("login");
        }

        try {
            String accessToken = getAccessToken(webProperties, providerName, StringUtils.join(redirectParts, "/"), oar, provider);
            /*long expiresIn = oauthResponse.getExpiresIn();
            if (expiresIn > 0 && expiresIn < System.currentTimeMillis()) {
                ActionErrors errors = new ActionErrors();
                errors.add(ActionErrors.GLOBAL_MESSAGE, new ActionMessage("oauth2.error.expired"));
                saveErrors(request, errors);
                return mapping.findForward("login");
            }*/

            DelegatedIdentity identity = getDelegatedIdentity(providerName, accessToken);

            ActionMessages messages = loginUser(request, identity);

            saveMessages(request, messages);
            return mapping.findForward("mymine");
        } catch (Exception e) {
            LOG.error("Error granting access", e);
            ActionErrors errors = new ActionErrors();
            errors.add(ActionErrors.GLOBAL_MESSAGE, new ActionMessage("oauth2.error.granting", e.getLocalizedMessage()));
            saveErrors(request, errors);
            return mapping.findForward("login");
        }
    }

    private String getAccessToken(Properties webProperties,
            String providerName, String redirectUri,
            OAuthAuthzResponse oar, OAuthProvider provider)
            throws OAuthSystemException, OAuthProblemException {
        OAuthClient oauthClient = new OAuthClient(new URLConnectionClient());
        // TODO: deal with different response types...
        OAuthAccessTokenResponse oauthResponse;
        OAuthClientRequest clientReq;
        TokenRequestBuilder requestBuilder = OAuthClientRequest
                .tokenLocation(provider.getTokenUrl())
                .setGrantType(GrantType.AUTHORIZATION_CODE)
                .setClientId(webProperties.getProperty("oauth2." + providerName + ".client-id"))
                .setClientSecret(webProperties.getProperty("oauth2." + providerName + ".client-secret"))
                .setRedirectURI(redirectUri)
                .setCode(oar.getCode());
        switch (provider.getMessageFormat()) {
        case BODY:
            clientReq = requestBuilder.buildBodyMessage();
            break;
        case QUERY:
            clientReq = requestBuilder.buildQueryMessage();
        default:
            throw new RuntimeException("Unknown message format");
        }
        LOG.debug("Sending token request: " + clientReq.getLocationUri());

        switch (provider.getResponseType()) {
        case FORM:
            oauthResponse = oauthClient.accessToken(clientReq, GitHubTokenResponse.class);
            break;
        case JSON:
            oauthResponse = oauthClient.accessToken(clientReq);
            break;
        default:
            throw new RuntimeException("Unknown response type");
        }
        String accessToken = oauthResponse.getAccessToken();
        return accessToken;
    }

    private OAuthProvider getProvider(Properties properties, String providerName) throws IllegalArgumentException {
        if (properties.containsKey("oauth2." + providerName + ".url.token")) {
            // Presence of this key implies all other options.
            return new CustomOAuthProvider(properties, providerName);
        } else {
            return new DefaultOAuthProvider(OAuthProviderType.valueOf(providerName));
        }
    }

    private ActionMessages loginUser(HttpServletRequest request, DelegatedIdentity identity) {
        LOG.info("Logging in " + identity);
        Profile currentProfile = SessionMethods.getProfile(request.getSession());
        InterMineAPI api = SessionMethods.getInterMineAPI(request.getSession());
        Profile profile = api.getProfileManager()
                             .grantPermission(identity.getProvider(), identity.getId(), api.getClassKeys())
                             .getProfile();
        profile.getPreferences().put(UserPreferences.EMAIL, identity.getEmail());
        profile.getPreferences().put(UserPreferences.AKA, identity.getName());

        ActionMessages messages = new ActionMessages();
        setUpProfile(request.getSession(), profile);
        messages.add(ActionMessages.GLOBAL_MESSAGE,
                new ActionMessage("login.oauth2.successful", identity.getProvider(), profile.getName()));
        Map<String, String> renamedBags = new HashMap<String, String>();

        if (currentProfile != null && StringUtils.isEmpty(currentProfile.getUsername())) {
            // The current profile was for an anonymous guest.
            renamedBags = mergeProfiles(currentProfile, profile);
        }
        if (!renamedBags.isEmpty()) {
            for (Entry<String, String> pair: renamedBags.entrySet()) {
                messages.add(ActionMessages.GLOBAL_MESSAGE,
                        new ActionMessage("login.renamed.bag", pair.getKey(), pair.getValue()));
            }
        }

        return messages;
    }

    private DelegatedIdentity getDelegatedIdentity(String providerName, String accessToken)
            throws OAuthSystemException, OAuthProblemException, JSONException {
        if (providerIsSane(providerName)) {
            return getSaneProviderUserInfo(providerName, accessToken);
        }
        throw new RuntimeException("I don't know how to get this information.");
    }

    private boolean providerIsSane(String providerName) {
        Properties webProperties = InterMineContext.getWebProperties();
        return webProperties.containsKey("oauth2." + providerName + ".identity-resource");
    }

    /**
     * Get user info for services which are sane enough to have an identity resource that serves json
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
        Properties webProperties = InterMineContext.getWebProperties();
        String identityEndpoint = webProperties.getProperty("oauth2." + provider + ".identity-resource");
        String nameKey = webProperties.getProperty("oauth2." + provider + ".name-key", "name");

        OAuthClientRequest bearerClientRequest =
                new OAuthBearerClientRequest(identityEndpoint).setAccessToken(accessToken).buildQueryMessage();
        bearerClientRequest.setHeader("Accept", "application/json");
        OAuthClient oauthClient = new OAuthClient(new URLConnectionClient());
        OAuthResourceResponse resourceResponse = oauthClient.resource(bearerClientRequest,
                OAuth.HttpMethod.GET, OAuthResourceResponse.class);

        JSONObject result = new JSONObject(resourceResponse.getBody());
        String id = result.get("id").toString();
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
            email = result.optString("email");
        } else {
            email = emails.optString("preferred");
            if (email == null) email = emails.optString("account");
        }
        return new DelegatedIdentity(provider, id, email, name);
    }
}
