package org.intermine.web.struts.oauth2;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.common.OAuthProviderType;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.struts.InterMineAction;

public class Authenticator extends InterMineAction {

    private static Logger LOG = Logger.getLogger(Authenticator.class);

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
    @Override
    public ActionForward execute(
            ActionMapping mapping,
            ActionForm form,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        OAuthClientRequest authRequest;
        OAuthProviderType provider;
        
        Properties webProperties = InterMineContext.getWebProperties();


        // Suitable values are: GOOGLE, GITHUB, FACEBOOK, etc.
        String providerName = request.getParameter("provider");

        List<String> redirectParts = new ArrayList<String>();
        redirectParts.add(webProperties.getProperty("webapp.baseurl"));
        redirectParts.add(webProperties.getProperty("webapp.path"));
        redirectParts.add("oauth2callback.do?provider=" + providerName);
        String state = UUID.randomUUID().toString();
        request.getSession().setAttribute("oauth2.state", state);

        String authorisationUrl = webProperties.getProperty("oauth2." + providerName + ".url.auth");
        if (authorisationUrl == null) {
            try {
                provider = OAuthProviderType.valueOf(providerName);
                authorisationUrl = provider.getAuthzEndpoint();
            } catch (IllegalArgumentException e) {
                ActionErrors errors = new ActionErrors();
                errors.add(ActionErrors.GLOBAL_MESSAGE, new ActionMessage("oauth2.error.unknown-provider"));
                saveErrors(request, errors);
                return mapping.findForward("login");
            }
        }
        try {
            authRequest = OAuthClientRequest
                           .authorizationLocation(authorisationUrl)
                           .setClientId(webProperties.getProperty("oauth2." + providerName + ".client-id"))
                           .setRedirectURI(StringUtils.join(redirectParts, "/"))
                           .setScope(webProperties.getProperty("oauth2." + providerName + ".scopes"))
                           .setState(state)
                           .buildQueryMessage();
            // various providers require the response_type parameter.
            LOG.info("Redirecting to " + authRequest.getLocationUri() + "&response_type=code");
            response.sendRedirect(authRequest.getLocationUri() + "&response_type=code");
            return null;
        } catch (OAuthSystemException e) {
            ActionErrors errors = new ActionErrors();
            errors.add(ActionErrors.GLOBAL_MESSAGE, new ActionMessage("oauth2.error.system-exception", e));
            saveErrors(request, errors);
            return mapping.findForward("login");
        }
    }
}
