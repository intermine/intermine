package org.intermine.util;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.Map;
import java.util.Properties;

import javax.mail.MessagingException;

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
	
}
