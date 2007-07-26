package org.intermine.web.struts;

/* 
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;
import java.util.Properties;

import java.text.MessageFormat;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Mail utilities for the webapp.
 * @author Kim Rutherford
 */
public abstract class MailUtils
{
    /**
     * Send a password to an email address
     * 
     * @param to the address to send to
     * @param imPassword the password to send
     * @param webProperties properties such as the from address
     * @throws Exception if there is a problem creating the email 
     */
    public static void email(String to, String imPassword, final Map webProperties)
        throws Exception {

        final String user = (String) webProperties.get("mail.smtp.user");
        String text = (String) webProperties.get("mail.text");
        String authFlag = (String) webProperties.get("mail.smtp.auth");

        text = MessageFormat.format(text, new Object[] { imPassword });
        Properties properties = System.getProperties();

        properties.put("mail.smtp.host", webProperties.get("mail.host"));
        properties.put("mail.smtp.user", user);
        properties.put("mail.smtp.starttls.enable", webProperties.get("mail.smtp.starttls.enable"));
        properties.put("mail.smtp.auth", authFlag);

        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                String password = (String) webProperties.get("mail.server.password");
                return new PasswordAuthentication(user, password);
            }
        };
        Session session;
        if (authFlag != null && (authFlag.equals("true") || authFlag.equals("t"))) {
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

}
