/**
 * 
 */
package org.intermine.api.profile;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.intermine.model.userprofile.UserProfile;
import org.intermine.util.PropertiesUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A class for interfacing to the JGI Caliban authentication API.
 * @author jcarlson
 *
 */
public class Caliban {

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
  
  static public HashMap<String,String> getIdentityHash(String token) throws
  MalformedURLException,SAXException,IOException
  {

    System.setProperty("javax.net.ssl.trustStore",
        PropertiesUtil.getProperties().getProperty("keystore.name"));
    System.setProperty("javax.net.ssl.trustStorePassword",
        PropertiesUtil.getProperties().getProperty("keystore.password"));
    
    // we make a couple requests to our authenticator API using this token
    // we get bac very simple documents, so we'll just DOM them.
    String signonURLBase = PropertiesUtil.getProperties().getProperty("caliban.signon");
    URL url = new URL(signonURLBase+"/api/sessions/"+token);
    DocumentBuilderFactory dBF = DocumentBuilderFactory.newInstance();
    DocumentBuilder dB;
    try {
      dB = dBF.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      // we really cannot expect this to be thrown.
      return null;
    }
    Document doc = dB.parse(url.openStream());
    NodeList nL = doc.getElementsByTagName("user");
    if (nL.item(0).getNodeType() == Node.ELEMENT_NODE) {
      String link = ((Element)nL.item(0)).getTextContent();
      URL userURL = null;
      userURL = new URL(signonURLBase+link);
      Document userDoc = dB.parse(userURL.openStream());
      // we got something. Time to fill it
      HashMap<String,String> newIdentity = new HashMap<String,String>();
      for(String tag : tagList ) {
        NodeList userNL = userDoc.getElementsByTagName(tag);
        if (userNL.item(0).getNodeType() == Node.ELEMENT_NODE) {
          newIdentity.put(tag, userNL.item(0).getTextContent());
        } else {
          newIdentity.put(tag,null);
        }
      }
      // and also stash the cookie in the identity hash. It will be the token.
      newIdentity.put("token",token);
      // and refresh the session
      HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
      httpCon.setDoOutput(true);
      httpCon.setRequestMethod("PUT");
      OutputStreamWriter out = new OutputStreamWriter(httpCon.getOutputStream());
      out.write("Resource content");
      out.close();
      httpCon.getInputStream();
      return newIdentity;

    }
    return null;
  }
  static public Profile createUserAccount(ProfileManager profileManager, HashMap<String,String> identity)
  {
    Profile prof =  new Profile(profileManager, identity.get("login"), null, null,
        new HashMap(), new HashMap(), new HashMap(), null, false, false);
    // this is looking at the internal hash directly.
    Map<String, String> preferences = prof.getPreferences();
    for(String key : savedPreferenceTags) {
      if (identity.containsKey(key) && !identity.get(key).isEmpty()) {
        preferences.put(key,identity.get(key));
      }
    }   

    profileManager.createProfile(prof);
    
    // (re)assign API token from jgi_session value
    String token = identity.get("token");
    prof.setApiKey(token);
    
    return prof;
  }
}
