package org.intermine.web.context;

import java.util.Properties;

import org.intermine.util.Emailer;

public class EmailerFactory {

    public static Emailer getEmailer(Properties webProperties) {
        if ("none".equals(webProperties.getProperty("mail.policy"))) {
            return new NullMailer();
        } else {
            return new Emailer(webProperties);
        }
    }
}
