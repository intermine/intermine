import java.lang.String;
import java.lang.StringBuffer;
import java.io.*;

import java.net.URL;
import java.security.cert.Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

 openssl s_client -connect signon.phytozone.net:443
 keytool -import -alias phytozome.net -file phytozome.cert -keystore ~/.keystore

public class testCaliban {
  public static void main(String args[]) throws Exception {

    System.setProperty("javax.net.ssl.trustStore","/global/u1/j/jcarlson/.keystore");
    System.setProperty("javax.net.ssl.trustStorePassword","password");

    URL url = new URL("https://signon.phytozome.net/api/sessions/cedbf65e68906e10");
    HttpsURLConnection con = (HttpsURLConnection)url.openConnection();

    System.out.println("Opened connection...");

    DocumentBuilderFactory dBF = DocumentBuilderFactory.newInstance();
    DocumentBuilder dB = null;
    StringBuffer responseString = new StringBuffer();
    try {
      dB = dBF.newDocumentBuilder();
      System.out.println("Got document builder.");
    } catch ( ParserConfigurationException e) {
      responseString.append("There was a problem getting login information");
      System.out.println(responseString);
      System.exit(0);
    }
    Document doc = null;
      try {
        //doc = dB.parse(con.getURL().openStream());
        con.connect();
        System.out.println("Called connect.");
        doc = dB.parse(con.getInputStream());
      } catch (SAXException e) {
        responseString.append("There was a problem parsing response.");
        System.out.println(responseString);
        System.exit(0);
      }
      NodeList nL = doc.getElementsByTagName("user");
      if (nL.item(0).getNodeType() == Node.ELEMENT_NODE){
        String link = ((Element)nL.item(0)).getTextContent();
          responseString.append("retrieving "+link);
          System.out.println("Link is "+link);
          URL userURL = new URL("https://signon.phytozome.net"+link);
          Document userDoc = null;
          try {
            userDoc = dB.parse(userURL.openStream());
          } catch ( SAXException e) {
            responseString.append("There was a problem parsing user info response.");
            System.out.println(responseString);
            System.exit(0);
          }
          NodeList fNL = userDoc.getElementsByTagName("first_name");
          NodeList lNL = userDoc.getElementsByTagName("last_name");
          if (fNL.item(0).getNodeType() == Node.ELEMENT_NODE &&
              lNL.item(0).getNodeType() == Node.ELEMENT_NODE ) {
            responseString.append("hello "+
              ((Element)fNL.item(0)).getTextContent() + " " +
              ((Element)lNL.item(0)).getTextContent() );
            System.out.println(responseString.toString());
          System.exit(0);
          }
        }
  }
}
