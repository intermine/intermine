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

import java.util.Arrays;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.intermine.util.Emailer;

/**
 * An emailer that doesn't send emails, but does log what it has been asked to do.
 * @author Alex Kalderimis
 *
 */
public class NullMailer extends Emailer
{

    private static final Logger LOG = Logger.getLogger(NullMailer.class);

    /**
     * Get a new NullMailer
     */
    public NullMailer() {
        super(new Properties());
    }

    /**
     * Get a null mailer with certain properties.
     * @param properties The properties.
     */
    public NullMailer(Properties properties) {
        super(properties);
    }

    @Override
    public void email(String to, String msgKey) {
        LOG.debug("Not sending message " + msgKey + " to " + to);
    }

    @Override
    public void email(String to, String msgKey, Object... vars) {
        LOG.debug("Not sending message " + msgKey + " to " + to
                + " with " + Arrays.toString(vars));
    }

    @Override
    public void welcome(String to) {
        LOG.debug("Not welcomming " + to);
    }

    @Override
    public String subscribeToList(String address) {
        LOG.debug("Not subscribing " + address + " to any list");
        return null;
    }
}
