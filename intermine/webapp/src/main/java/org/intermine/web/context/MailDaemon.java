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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.intermine.util.Emailer;

/**
 * An runnable that will send emails delivered over a concurrent queue.
 * @author Alex Kalderimis
 *
 */
public final class MailDaemon implements Runnable
{

    private static final Logger LOG = Logger.getLogger(MailDaemon.class);
    private final ArrayBlockingQueue<MailAction> mailQueue;
    private final Emailer emailer;

    /**
     * Create a MailDaemon
     * @param mailQueue The source for mail actions.
     * @param emailer The emailer to fob them off onto.
     */
    public MailDaemon(ArrayBlockingQueue<MailAction> mailQueue, Emailer emailer) {
        this.mailQueue = mailQueue;
        this.emailer = emailer;
    }

    @Override
    public void run() {
        while (true) {
            if (Thread.interrupted()) {
                return; // Shutdown.
            }
            try {
                // Wait here until there is a message to send.
                MailAction nextAction = mailQueue.poll(1, TimeUnit.SECONDS);
                // Send it.
                if (nextAction != null) {
                    nextAction.act(emailer);
                }
            } catch (InterruptedException e) {
                return; // Most likely shutdown.
            } catch (Exception e) {
                LOG.error("Could not send email", e);
            }
        }
    }

}
