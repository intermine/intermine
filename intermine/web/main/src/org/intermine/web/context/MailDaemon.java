package org.intermine.web.context;

import java.util.concurrent.ArrayBlockingQueue;

import javax.mail.MessagingException;

import org.apache.log4j.Logger;
import org.intermine.util.Emailer;

public class MailDaemon implements Runnable {

    private static final Logger LOG = Logger.getLogger(MailDaemon.class);
    private final ArrayBlockingQueue<MailAction> mailQueue;
    private final Emailer emailer; 

    public MailDaemon(ArrayBlockingQueue<MailAction> mailQueue, Emailer emailer) {
        this.mailQueue = mailQueue;
        this.emailer = emailer;
    }

    @Override
    public void run() {
        while (true) {
            try {
                // Wait here until there is a message to send.
                MailAction nextAction = mailQueue.take();
                // Send it.
                nextAction.act(emailer);
            } catch (InterruptedException e) {
                return; // Most likely shutdown.
            } catch (Exception e) {
                LOG.error("Could not send email", e);
            }
        }
    }

}
