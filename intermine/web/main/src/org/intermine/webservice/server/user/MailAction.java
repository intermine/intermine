package org.intermine.webservice.server.user;

public class MailAction {

	private final Message message;
	private final String address;

	public MailAction(Message message, String username) {
		this.message = message;
		this.address = username;
	}

	public Message getMessage() {
		return message;
	}

	public String getAddress() {
		return address;
	}

	public enum Message { WELCOME, SUBSCRIBE };

}
