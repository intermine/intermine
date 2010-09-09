package org.intermine.util;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.text.MessageFormat;
import java.util.Map;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Mail utilities for the webapp.
 *
 * @author Kim Rutherford
 * @author Matthew Wakeling
 */
public abstract class MailUtils
{
    private MailUtils() {
    }

    /**
     * Send a welcoming email to an email address
     *
     * @param to the address to send to
     * @param webProperties properties such as the from address
     * @throws Exception if there is a problem creating the email
     */
    public static void email(String to, final Map webProperties) throws Exception {
        final String user = (String) webProperties.get("mail.smtp.user");
        String smtpPort = (String) webProperties.get("mail.smtp.port");
        String text = (String) webProperties.get("mail.text");
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
        message.setFrom(new InternetAddress(((String) webProperties.get("mail.from"))));
        message.addRecipient(Message.RecipientType.TO, InternetAddress.parse(to, true)[0]);
        message.setSubject((String) webProperties.get("mail.subject"));
        message.setContent(text, "text/plain");
        Transport.send(message);
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
        final String user = (String) webProperties.get("mail.smtp.user");
        String smtpPort = (String) webProperties.get("mail.smtp.port");
        String mailText = (String) webProperties.get("mail.passwordText");
        String authFlag = (String) webProperties.get("mail.smtp.auth");
        String starttlsFlag = (String) webProperties.get("mail.smtp.starttls.enable");

        Properties properties = System.getProperties();
        mailText = MessageFormat.format(mailText, new Object[] {url});

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

        String projectTitle = (String) webProperties.get("project.title");
        String baseSubject = (String) webProperties.get("mail.passwordSubject");
        String mailSubject = MessageFormat.format(baseSubject, new Object[] {projectTitle});

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(((String) webProperties.get("mail.from"))));
        message.addRecipient(Message.RecipientType.TO, InternetAddress.parse(to, true)[0]);
        message.setSubject(mailSubject);
        message.setContent(mailText, "text/plain");
        Transport.send(message);
    }


    /**
     * Subscribe the given email address to the mailing list specified in the
     * mine config file
     * @param email the email to subscribe
     * @param webProperties the web properties
     * @throws Exception when somethign goes wrong
     */
    public static void subscribe(String email, final Map webProperties) throws Exception {
        final String user = (String) webProperties.get("mail.smtp.user");
        String smtpPort = (String) webProperties.get("mail.smtp.port");
        String authFlag = (String) webProperties.get("mail.smtp.auth");
        String starttlsFlag = (String) webProperties.get("mail.smtp.starttls.enable");

        Properties properties = System.getProperties();

        properties.put("mail.smtp.host", webProperties.get("mail.host"));
        properties.put("mail.smtp.user", user);
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
        message.setFrom(new InternetAddress(email));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(((String) webProperties
                        .get("mail.mailing-list"))));
        message.setContent("", "text/plain");
        Transport.send(message);
    }
}
