package org.intermine.web.logic.login;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.expressme.openid.Association;
import org.expressme.openid.Authentication;
import org.expressme.openid.Endpoint;
import org.expressme.openid.OpenIdException;
import org.expressme.openid.OpenIdManager;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.LoginHandler;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Authenticate requests using OpenID
 * @author Alexis Kalderimis
 */
public class OpenIDAuthenticator extends HttpServlet
{

    private static final long serialVersionUID = -3591074522737892280L;
    static final long ONE_HOUR = 3600000L;
    static final long TWO_HOUR = ONE_HOUR * 2L;
    static final String ATTR_MAC = "openid_mac";
    static final String ATTR_ALIAS = "openid_alias";
    static final String PARAM_NONCE = "openid.response_nonce";
    static final String PARAM_PROVIDER = "provider";
    static final Set<String> NONCES = new HashSet<String>();

    private String loginUrl;
    private String returnTo;
    private OpenIdManager manager;
    private ProfileManager profileManager;

    /**
     * Default constructor.
     */
    public OpenIDAuthenticator() {
        // Empty Constructor
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        manager = new OpenIdManager();
        ServletContext context = config.getServletContext();
        Properties webProperties = SessionMethods.getWebProperties(context);
        String prefix = webProperties.getProperty("webapp.baseurl");
        String path = webProperties.getProperty("webapp.path");
        manager.setRealm(prefix);
        returnTo = prefix + "/" + path + "/openid";
        manager.setReturnTo(returnTo);
        profileManager = SessionMethods.getInterMineAPI(context).getProfileManager();
        loginUrl = prefix + "/" + path + "/mymine.do";
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String provider = request.getParameter(PARAM_PROVIDER);

        if (provider == null) {
            checkNonce(request.getParameter(PARAM_NONCE));
            byte[] mackey = (byte[]) request.getSession().getAttribute(ATTR_MAC);
            String alias = (String) request.getSession().getAttribute(ATTR_ALIAS);
            Authentication auth = manager.getAuthentication(request, mackey, alias);
            String email = auth.getEmail();
            String fullName = auth.getFullname();
            HttpSession s = request.getSession();
            if (fullName != null) {
                s.setAttribute(Constants.USERNAME, fullName);
            } else {
                s.setAttribute(Constants.USERNAME, email);
            }
            String identity = auth.getIdentity();
            if (!profileManager.hasProfile(identity)) {
                profileManager.createProfile(
                        new Profile(profileManager, identity, null, null,
                                new HashMap(), new HashMap(), new HashMap(), null, false, false));
            }
            LoginHandler.doStaticLogin(request, identity, null);
            response.sendRedirect(loginUrl);
        } else {
             // redirect to provider sign on page:
            Endpoint endpoint = manager.lookupEndpoint(provider);
            request.getSession().setAttribute(Constants.PROVIDER, provider);
            Association association = manager.lookupAssociation(endpoint);
            request.getSession().setAttribute(ATTR_MAC, association.getRawMacKey());
            request.getSession().setAttribute(ATTR_ALIAS, endpoint.getAlias());
            String url = manager.getAuthenticationUrl(endpoint, association);
            response.sendRedirect(url);
        }
    }

    private void checkNonce(String nonce) {
        if (nonce == null || nonce.length() < 20) {
            throw new OpenIdException("Verify failed - bad nonce");
        }
        if (NONCES.contains(nonce)) {
            throw new OpenIdException("Verify failed - nonce has previously been used");
        }
        long nonceTime = getNonceTime(nonce);
        long diff = System.currentTimeMillis() - nonceTime;
        if (diff < 0) {
            diff = -diff;
        }
        if (diff > ONE_HOUR) {
            throw new OpenIdException("Verify failed - expired nonce");
        }
        NONCES.add(nonce);
    }

    private long getNonceTime(String nonce) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                    .parse(nonce.substring(0, 19) + "+0000")
                    .getTime();
        } catch (ParseException e) {
            throw new OpenIdException("Bad nonce time.");
        }
    }

}
