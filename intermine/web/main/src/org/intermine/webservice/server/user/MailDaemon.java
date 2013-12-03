package org.intermine.webservice.server.user;

import java.util.concurrent.ArrayBlockingQueue;

import javax.mail.MessagingException;

import org.apache.log4j.Logger;
import org.intermine.util.Emailer;
import org.intermine.web.context.InterMineContext;

public class MailDaemon implements Runnable {

	private static final Logger LOG = Logger.getLogger(MailDaemon.class);
	private final ArrayBlockingQueue<MailAction> mailQueue;
	private final Emailer emailer; 

	public MailDaemon(ArrayBlockingQueue<MailAction> mailQueue) {
		this.mailQueue = mailQueue;
		emailer = InterMineContext.getEmailer();
	}

	@Override
	public void run() {
		while (true) {
			try {
				MailAction nextAction = mailQueue.take();
				switch (nextAction.getMessage()) {
				case WELCOME:
					emailer.welcome(nextAction.getAddress());
					break;
				case SUBSCRIBE:
					emailer.subscribeToList(nextAction.getAddress());
					break;
				}
			} catch (InterruptedException e) {
				return; // Most likely shutdown.
			} catch (MessagingException e) {
				LOG.error("Could not send email", e);
			}
		}
	}

}
