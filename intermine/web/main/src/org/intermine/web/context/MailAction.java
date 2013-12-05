package org.intermine.web.context;

import javax.mail.MessagingException;

import org.intermine.util.Emailer;

public interface MailAction {

    public void act(Emailer emailer) throws Exception;
}
