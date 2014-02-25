package org.intermine.web.context;

import java.util.Arrays;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.intermine.util.Emailer;

public class NullMailer extends Emailer {

    private static final Logger LOG = Logger.getLogger(NullMailer.class);
    public NullMailer() {
        super(new Properties());
    }

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
