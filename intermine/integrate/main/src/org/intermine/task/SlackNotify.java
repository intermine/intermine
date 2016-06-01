package org.intermine.task;


import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;


public class SlackNotify extends Task {
  
  private Properties projectProperties;
  private String sourceAttribute = "all sources.";

  /**
   * Set and load properties. The propertiesFile must lie in the classpath.
   * @param propertiesFile
   */
  public void setProperties(String propertiesFile) {
    projectProperties = new Properties();
    try {
      ClassLoader cl = SlackNotify.class.getClassLoader();
      InputStream is = cl.getResourceAsStream(propertiesFile);
      if (is == null) {
        throw new BuildException("Could not find file " + propertiesFile);
      }
      projectProperties.load(is);
      is.close();
    } catch (IOException e) {
      throw new BuildException("Failed to load :" + propertiesFile, e);
    }
  }

  /**
   * Set the source from integrate.
   * @param source the source
   */
  public void setSource(String source) {
      this.sourceAttribute = source.replaceAll(" ","+");
  }
  
  public void execute() throws BuildException {
    
    String url = "https://slack.com/api/chat.postMessage";
    URL obj;
    String token = projectProperties.getProperty("slack.token");
    String channel = projectProperties.getProperty("slack.channel");

    if (token == null || token.equals("") || channel == null || channel.equals("") ) {
      // if these are not set, we'll quietly return without doing anything
      return;
    }

    System.out.println("Sending notification for completion of "+sourceAttribute);
    String urlParameters = "token="+token+"&channel="+channel+"&text=Loaded+source+"+sourceAttribute+"&username=InterMine+Builder";
    
    HttpsURLConnection con;
    try {
      obj = new URL(url);
      con = (HttpsURLConnection) obj.openConnection();
      //add header
      con.setRequestMethod("POST");
      // Send post request
      con.setDoOutput(true);
      DataOutputStream wr = new DataOutputStream(con.getOutputStream());
      wr.writeBytes(urlParameters);
      wr.flush();
      wr.close();

    } catch (Exception e) {
      throw new BuildException("Exception while sending message in notify task: "+e.getMessage());
    }

    BufferedReader in;
    String inputLine;
    StringBuffer response = new StringBuffer();
    try {
      in = new BufferedReader(new InputStreamReader(con.getInputStream()));
      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
      }
      in.close();
    } catch (Exception e) {
      throw new BuildException("Exception while receiving message in notify task: "+e.getMessage());
    }

  }
}
