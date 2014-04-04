/**
 *
 */
package org.intermine.web.logic.login;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import org.apache.log4j.Logger;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.Caliban;
import org.intermine.util.PropertiesUtil;
import org.intermine.web.logic.profile.LoginHandler;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.InterMineAction;


/**
 * @author jcarlson
 *
 */
public class CalibanAuthenticator extends HttpServlet {

  static Logger log = Logger.getLogger(CalibanAuthenticator.class);
  // items provided by caliban
  static String[] tagList = {"login", "email", "id", "prefix", "first_name", "middle_name",
    "last_name", "suffix", "gender", "institution", "department", "address_1",
    "address_2", "city", "state", "postal_code", "country", "phone_number",
    "fax_number", "updated_at", "contact_id"};
  // from the above list, the ones that get saved in userpreferences
  static String[] savedPreferenceTags = {"email", "prefix", "first_name", "middle_name",
    "last_name", "suffix", "gender", "institution", "department", "address_1",
    "address_2", "city", "state", "postal_code", "country", "phone_number",
  "fax_number"};
  // we'll fill this in from caliban after login.
  HashMap<String,String> identity = new HashMap<String,String>();
  private ProfileManager profileManager;
  private String calibanSessionId = null;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    //System.setProperty("javax.net.ssl.trustStore",
    //    InterMineAction.getWebProperties(request).getProperty("keystore.name"));
    //System.setProperty("javax.net.ssl.trustStorePassword",
    //    InterMineAction.getWebProperties(request).getProperty("keystore.password"));
    ServletContext context = config.getServletContext();
    System.setProperty("javax.net.ssl.trustStore",
        PropertiesUtil.getProperties().getProperty("keystore.name"));
    System.setProperty("javax.net.ssl.trustStorePassword",
        PropertiesUtil.getProperties().getProperty("keystore.password"));
    profileManager = SessionMethods.getInterMineAPI(context).getProfileManager();
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    String returnTo = request.getParameter("returnto");
    if( returnTo==null || returnTo.isEmpty()) returnTo = "/begin.do";


    
    if (!hasIdentity(request,response) ) {
      // this person is not logged in. 
      if ((request.getParameter("checkonly") != null) &&
          request.getParameter("checkonly").equals("1")) {
        // If we're just checking, then move on.
        log.debug("Not logged in, but just checking. off to "+
            InterMineAction.getWebProperties(request).getProperty("project.sitePrefix")+ returnTo);
        response.sendRedirect(
            InterMineAction.getWebProperties(request).getProperty("project.sitePrefix")+ returnTo);
      } else {
        // off to the authenticator
        Cookie cookie = new Cookie("jgi_return",
            URLEncoder.encode(
                InterMineAction.getWebProperties(request).getProperty("project.sitePrefix")+
                "/caliban?returnto="+returnTo,"UTF-8"));
        //log.info("Setting jgi_return cookie to "+cookie.getValue());
        cookie.setDomain(InterMineAction.getWebProperties(request).getProperty("project.siteDomain"));
        cookie.setPath("/");
        response.addCookie(cookie);
        response.sendRedirect(InterMineAction.getWebProperties(request).getProperty("caliban.signon"));
      }
    } else {
      Profile prof;
      // examine the identity hash to see if there is an account set for this 'login'
      if (!profileManager.hasProfile(identity.get("login"))) {
        // if not, create this account.
        prof = Caliban.createUserAccount(profileManager,identity);
      } else {
        prof = profileManager.getProfile(identity.get("login"));
      }
      // (re)assign API token from jgi_session value
      String token = identity.get("token");
      prof.setApiKey(token);
      LoginHandler.doStaticLogin(request, identity.get("login"), null);
      response.sendRedirect(
          InterMineAction.getWebProperties(request).getProperty("project.sitePrefix")+returnTo);
    }
  }

  /*
   * see if caliban can authorize that we are a legitimate user.
   * This returns true if caliban has authorized us. The identity hash
   * will be populated with everything we can find out about the user.
   */

  public boolean hasIdentity(HttpServletRequest request,
      HttpServletResponse response) {
    
    // first, see if there is a jgi_session cookie
    if ( (request == null) || (request.getCookies() == null) ) {
      return false;
    }
    
    for( Cookie cookie : request.getCookies() ) {
      //log.debug("Examining cookie "+cookie.getName()+" with value "+cookie.getValue());
      if (cookie.getName().equals("jgi_session")) {
        calibanSessionId = cookie.getValue();
        if (calibanSessionId.isEmpty()) {
          return false;
        }
        // just the token
        calibanSessionId = calibanSessionId.replace("%2Fapi%2Fsessions%2F","");
        try {
          identity = Caliban.getIdentityHash(calibanSessionId);
        } catch (IOException e) {
          log.error("There was a IO exception: " + e.getMessage());
          return false;
        } catch (SAXException e) {
          log.error("There was a problem parsing response: "+e.getMessage());
          return false;
        }
        // return true if we have a login.
        return identity != null && identity.containsKey("login") && !identity.get("login").isEmpty();
      }
    }
    return false;
  }

}
