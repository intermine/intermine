package org.intermine.util;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.intermine.api.profile.InterMineBag;

/**
 * Mail utilities for the webapp.
 *
 * @author Kim Rutherford
 * @author Matthew Wakeling
 */
public abstract class MailUtils
{
    private MailUtils() {
        // private constructor
    }

    /**
     * Send a welcoming email to an email address
     *
     * @param to the address to send to
     * @param webProperties properties such as the from address
     * @throws Exception if there is a problem creating the email
     */
    public static void welcome(String to, final Map webProperties) throws MessagingException {
        String subject = (String) webProperties.get("mail.subject");
        String text = (String) webProperties.get("mail.text");
        email(to, subject, text, webProperties);
    }

    /**
     * Send an email to an address, supplying the recipient, subject and body.
     *
     * @param to the address to send to
     * @param subject The Subject of the email
     * @param body The content of the email
     * @param from the address to send from
     * @param webProperties Common properties for all emails (such as from, authentication)
     * @throws MessagingException if there is a problem creating the email
     */
    public static void email(String to, String subject, String body, String from,
            final Map webProperties) throws MessagingException {
        final String user = (String) webProperties.get("mail.smtp.user");
        String smtpPort = (String) webProperties.get("mail.smtp.port");
        String authFlag = (String) webProperties.get("mail.smtp.auth");
        String starttlsFlag = (String) webProperties.get("mail.smtp.starttls.enable");

        Properties properties = System.getProperties();

        properties.put("mail.smtp.host", webProperties.get("mail.host"));
        properties.put("mail.smtp.user", user);
        // Fix to "javax.mail.MessagingException: 501 Syntactically
        // invalid HELO argument(s)" problem
        // See http://forum.java.sun.com/thread.jspa?threadID=487000&messageID=2280968
        properties.put("mail.smtp.localhost", "localhost");
        if (smtpPort != null) {
            properties.put("mail.smtp.port", smtpPort);
        }

        if (starttlsFlag != null) {
            properties.put("mail.smtp.starttls.enable", starttlsFlag);
        }
        if (authFlag != null) {
            properties.put("mail.smtp.auth", authFlag);
        }

        Session session;
        if (authFlag != null && ("true".equals(authFlag) || "t".equals(authFlag))) {
            Authenticator authenticator = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    String password = (String) webProperties.get("mail.server.password");
                    return new PasswordAuthentication(user, password);
                }
            };
            session = Session.getDefaultInstance(properties, authenticator);
        } else {
            session = Session.getDefaultInstance(properties);
        }
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.addRecipient(Message.RecipientType.TO, InternetAddress.parse(to, true)[0]);
        message.setSubject(subject);
        message.setContent(body, "text/plain");
        Transport.send(message);
    }

    /**
     * @param to the address to send to
     * @param subject The Subject of the email
     * @param body The content of the email
     * @param webProperties Common properties for all emails (such as from, authentication)
     * @throws MessagingException if there is a problem creating the email
     */
    public static void email(String to, String subject, String body, final Map webProperties)
        throws MessagingException {
        String from = (String) webProperties.get("mail.from");
        email(to, subject, body, from, webProperties);
    }

    /**
     * Send a password change email to an email address
     *
     * @param to the address to send to
     * @param url the URL to embed in the email
     * @param webProperties properties such as the from address
     * @throws Exception if there is a problem creating the email
     */
    public static void emailPasswordToken(String to, String url, final Map webProperties)
        throws Exception {

        String projectTitle = (String) webProperties.get("project.title");
        String baseSubject = (String) webProperties.get("mail.passwordSubject");
        String mailSubject = MessageFormat.format(baseSubject, new Object[] {projectTitle});
        String mailText = (String) webProperties.get("mail.passwordText");
        String mailTextWithUrl = MessageFormat.format(mailText, new Object[] {url});
        email(to, mailSubject, mailTextWithUrl, webProperties);
    }


    /**
     * Subscribe the given email address to the mailing list specified in the
     * mine config file
     * @param email the email to subscribe
     * @param webProperties the web properties
     * @throws Exception when somethign goes wrong
     */
    public static void subscribe(String email, final Map webProperties) throws MessagingException {
        String to = (String) webProperties.get("mail.mailing-list");
        String subject = "";
        String body = "";
        email(to, subject, body, email, webProperties);
    }

    /**
     * Send a 'sharing list' message
     *
     * @param to the address to send to
     * @param sharingUser the user sharing the list
     * @param bag the list shared
     * @param webProperties properties such as the from address
     * @throws Exception if there is a problem creating the email
     */
    public static void emailSharingList(String to, String sharingUser, InterMineBag bag,
        final Map webProperties) throws Exception {
        String applicationName = (String) webProperties.get("project.title");
        String subject = applicationName + " shared list (" + bag.getName() + ")";
        String listUrl = webProperties.get("webapp.deploy.url") + "/"
                  +  webProperties.get("webapp.path") + "/"
                  + "login.do?returnto=%2FbagDetails.do%3Fscope%3Dall%26bagName%3D"
                  + URLEncoder.encode(bag.getName(), "UTF-8");
        StringBuffer bodyMsg = new StringBuffer();
        bodyMsg.append("User " + sharingUser + " has shared a list of " + bag.getType() + "s ");
        bodyMsg.append("with you called \"" + bag.getName() + "\".\n\n");
        bodyMsg.append("Click here to open and view the list: " + listUrl + ".\n\n");
        bodyMsg.append("You cannot delete or modify this list yourself. However, you may COPY "
                      + "this list to your own account by using the list operations on the list page.\n");
        bodyMsg.append("If " + sharingUser + " deletes or modifies this list, you will not be ");
        bodyMsg.append("notified.\n\n");
        bodyMsg.append("If you have any problems or questions, please don't hesitate ");
        bodyMsg.append("to contact us. We can be reached by replying to this email or at the ");
        bodyMsg.append("bottom of each page on " + applicationName + ".\n\n");
        bodyMsg.append("Thank you.\nThe " + applicationName + " team");
        email(to, subject, bodyMsg.toString(), webProperties);
    }
}
