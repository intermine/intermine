package org.intermine.util;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Properties;

import javax.mail.MessagingException;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.objectstore.ObjectStoreException;

/**
 * A class that will send emails to people.
 * @author Alex Kalderimis
 *
 */
public class Emailer
{

    private static final String PREFIX = "mail.regarding.";

    private final Properties properties;

    /**
     * Instantiate an emailer.
     * @param properties the configuration properties.
     */
    public Emailer(Properties properties) {
        this.properties = properties;
    }

    /**
     * Send a message to a user.
     * @param to The recipient
     * @param messageKey The key for the message.
     * @throws MessagingException if something goes wrong.
     */
    public void email(String to, String messageKey) throws MessagingException {
        String subject = properties.getProperty(PREFIX + messageKey + ".subject");
        String body = properties.getProperty(PREFIX + messageKey + ".body");
        MailUtils.email(to, subject, body, properties);
    }

    /**
     * Send a message to a user with some data.
     * @param to The recipient.
     * @param messageKey The message key.
     * @param vars The data.
     * @throws MessagingException If something goes wrong.
     */
    public void email(String to, String messageKey, Object... vars) throws MessagingException {
        String subject = properties.getProperty(PREFIX + messageKey + ".subject");
        String bodyFmt = properties.getProperty(PREFIX + messageKey + ".body");
        if (StringUtils.isBlank(bodyFmt)) {
            throw new MessagingException("No email template configured for " + messageKey);
        }
        String body = String.format(bodyFmt, vars);
        MailUtils.email(to, subject, body, properties);
    }

    /**
     * Welcome a user to the system.
     * @param to The new user.
     * @throws MessagingException If something goes wrong.
     */
    public void welcome(String to) throws MessagingException {
        MailUtils.welcome(to, properties);
    }

    /**
     * Bid a user farewell from the system.
     * @param address The former user.
     * @param xml All the user's stuff.
     * @throws MessagingException If something goes wrong.
     */
    public void sendFareWell(String address, String xml) throws MessagingException {
        String appName = properties.getProperty("project.title");
        String subjectFmt = properties.getProperty(PREFIX + "farewell.subject");
        String subject = String.format(subjectFmt, appName);

        String bodyFmt = properties.getProperty(PREFIX + "farewell.body");

        String body = String.format(bodyFmt, appName, address, xml);

        MailUtils.email(address, subject, body, properties);
    }

    /**
     * Subscribe a user to a mailing list.
     * @param address The address to subscribe.
     * @return The address of the mailing list.
     * @throws MessagingException if something goes wrong.
     */
    public String subscribeToList(String address) throws MessagingException {
        String mailingList = properties.getProperty("mail.mailing-list");
        if (!isBlank(mailingList)) {
            MailUtils.subscribe(address, properties);
            return mailingList;
        }
        return null;
    }

    /**
     * Send a 'sharing list' message
     *
     * @param to the address to send to
     * @param owner the user sharing the list
     * @param bag the list shared
     * @throws MessagingException if there is a problem creating the email
     * @throws ObjectStoreException if the bag cannot read its size
     * @throws UnsupportedEncodingException If UTF-8 is not supported.
     */
    public void informUserOfNewSharedBag(
            String to,
            Profile owner,
            InterMineBag bag)
        throws MessagingException, ObjectStoreException, UnsupportedEncodingException {
        String appName = properties.getProperty("project.title");
        String subjectFmt = properties.getProperty(PREFIX + "newly.shared.subject");
        String subject = String.format(subjectFmt, appName, owner.getUsername());

        String bodyFmt = properties.getProperty(PREFIX + "newly.shared.body");
        String base = properties.getProperty("webapp.baseurl");
        String path = properties.getProperty("webapp.path");

        String body = String.format(bodyFmt, appName, owner.getUsername(),
                bag.getType(), bag.getSize(), bag.getName(), base, path,
                URLEncoder.encode(bag.getName(), "UTF-8"));

        MailUtils.email(to, subject, body, properties);
    }


}
