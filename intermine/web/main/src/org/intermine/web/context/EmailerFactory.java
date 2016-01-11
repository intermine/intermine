package org.intermine.web.context;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Properties;

import org.intermine.util.Emailer;

/** @author Alex Kalderimis **/
public final class EmailerFactory
{

    private EmailerFactory() {
        // don't!
    }

    /**
     * @param webProperties Email configuration options.
     * @return an Emailer
     **/
    public static Emailer getEmailer(Properties webProperties) {
        if ("none".equals(webProperties.getProperty("mail.policy"))) {
            return new NullMailer();
        } else {
            return new Emailer(webProperties);
        }
    }
}
