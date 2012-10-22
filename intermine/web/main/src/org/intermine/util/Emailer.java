package org.intermine.util;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Properties;

import javax.mail.MessagingException;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.objectstore.ObjectStoreException;

public class Emailer {

    private static final String PREFIX = "mail.regarding.";
    
    private final Properties properties;
    
    public Emailer(Properties properties) {
        this.properties = properties;
        
    }
    
    public void email(String to, String messageKey) throws MessagingException {
        String subject = properties.getProperty(PREFIX + messageKey + ".subject");
        String body = properties.getProperty(PREFIX + messageKey + ".body");
        MailUtils.email(to, subject, body, properties);
    }
    
    public void email(String to, String messageKey, Object... vars) throws MessagingException {
        String subject = properties.getProperty(PREFIX + messageKey + ".subject");
        String bodyFmt = properties.getProperty(PREFIX + messageKey + ".body");
        if (StringUtils.isBlank(bodyFmt)) {
            throw new MessagingException("No email template configured for " + messageKey);
        }
        String body = String.format(bodyFmt, vars);
        MailUtils.email(to, subject, body, properties);
    }
    
    public void welcome(String to) throws MessagingException {
        MailUtils.welcome(to, properties);
    }
    
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
     * @param sharingUser the user sharing the list
     * @param bag the list shared
     * @throws MessagingException if there is a problem creating the email
     * @throws ObjectStoreException if the bag cannot read its size
     * @throws UnsupportedEncodingException If UTF-8 is not supported.
     */
    public void informUserOfNewSharedBag(String to, Profile owner, InterMineBag bag)
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
