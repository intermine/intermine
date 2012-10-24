package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.MessagingException;

import org.apache.log4j.Logger;
import org.intermine.util.MailUtils;
import org.intermine.util.PropertiesUtil;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A class to handle registration of mines with a registry service.
 * @author Alexis Kalderimis
 *
 */
public class Registrar extends Thread
{

    private static final Logger LOG = Logger.getLogger(Registrar.class);
    private static final String NEWLINE = System.getProperty("line.separator");

    private static final String REG_PROP_PREFIX = "registration.parameter";

    // These HTTP statuses are used by the webapp to distinguish
    // between creation and updates
    private static final String CREATED = "created"; // 201
    private static final String UPDATED = "ok"; // 200

    private final Properties props;

    private String emailContent = "";
    private final String emailIntro = "There were some problems "
        + "with the registration of your mine:" + NEWLINE;
    private final String subject = "Mine Registration Failed";

    /**
     * Constructor
     * @param webProperties The properties to configure this mine.
     */
    public Registrar(Properties webProperties) {
        this.props = webProperties;
        setName("Registrar");
    }

    private String getQueryString(Map<String, String> params)
        throws UnsupportedEncodingException {
        String data = "";
        for (String key: params.keySet()) {
            String value = params.get(key);
            if (value == null) { continue; }
            if (!data.isEmpty()) {
                data += "&";
            }
            data += URLEncoder.encode(key, "UTF-8");
            data += "=";
            data += URLEncoder.encode(value, "UTF-8");
        }
        return data;
    }

    private void populateParams(Map<String, String> params) {
        Properties paramProps = PropertiesUtil.stripStart(REG_PROP_PREFIX,
                PropertiesUtil.getPropertiesStartingWith(REG_PROP_PREFIX, props));
        for (Object key: paramProps.keySet()) {
            params.put(key.toString(), paramProps.getProperty((String) key));
        }
    }

    private void handleProblem(String problem) {
        LOG.error(problem);
        emailContent += " * " + problem + NEWLINE;
    }

    /**
     * Send a post request to the registry service, if one has been set up.
     * As Registrar extends Thread, the request will be executed in a separate Thread.
     */
    @Override
    public void run() {
        // Check to see whether we should run at all
        String registryAddress = props.getProperty("registration.address");
        if (registryAddress == null || registryAddress.isEmpty()) {
            LOG.info("No registry address supplied: "
                     + "will not attempt to register.");
            return;
        }
        String authToken = props.getProperty("registration.authToken");
        if (authToken == null || authToken.isEmpty()) {
            LOG.info("No registration authentication supplied: "
                     + "will not attempt to register.");
            return;
        }

        // Get the data to send to the server
        Map<String, String> params = new HashMap<String, String>();

        params.put("name", props.getProperty("project.title"));
        params.put("description", props.getProperty("project.subtitle"));
        params.put("authToken", authToken);
        params.put("format", "json");
        params.put("url", props.getProperty("webapp.baseurl")
                            + "/" + props.getProperty("webapp.path"));
        populateParams(params);

        try {
            Thread.sleep(60000);
            URL registry = new URL(registryAddress);
            String data = getQueryString(params);

            // Send data
            URLConnection conn = registry.openConnection();
            conn.setDoOutput(true);
            Writer out = new OutputStreamWriter(conn.getOutputStream());
            out.write(data);
            out.flush();
            out.close();

            // Get the response
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            rd.close();

            String response = sb.toString();

            // Handle the response
            LOG.debug("Registration response: " + response);
            JSONObject jo = new JSONObject(response);
            String status = jo.getString("status");
            if (CREATED.equals(status)) {
                String receivedToken = jo.getString("authToken");
                if (!authToken.equals(receivedToken)) {
                    handleProblem(
                        "Mine registered incorrectly: "
                        + "authToken does not match the one supplied. "
                        + "I gave " + authToken + ", but got back "
                        + receivedToken);
                } else {
                    LOG.info("Mine successfully registered");
                }
            } else if (UPDATED.equals(status)) {
                LOG.info("Mine registration details successfully updated");
            } else {
                handleProblem("Problem registering mine: "
                        + jo.getString("text"));
            }

        } catch (InterruptedException e) {
            handleProblem("Registration was interrupted" + e);
        } catch (UnsupportedEncodingException e) {
            handleProblem("Could not encode query string for parameters: " + params);
        } catch (MalformedURLException e) {
            handleProblem("Could not connect to registry - bad address: " + registryAddress);
        } catch (JSONException e) {
            handleProblem("Problem registering mine - server returned bad response: " + e);
        } catch (IOException e) {
            handleProblem("Problem connecting to registry: " + e);
        } catch (Exception e) {
            handleProblem("Unanticipated problem encountered registering mine: " + e);
        }
        if (!emailContent.isEmpty()) {
            String feedbackEmail = props.getProperty("registration.emailto");
            if (feedbackEmail == null) {
                feedbackEmail = props.getProperty("superuser.account");
            }
            try {
                MailUtils.email(feedbackEmail, subject, emailIntro + emailContent, props);
            } catch (MessagingException e) {
                LOG.error("Problem emailing information about problems to " + feedbackEmail);
            }
        }
    }
}
