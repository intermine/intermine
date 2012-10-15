package org.intermine.webservice.server.lists;

import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.core.WebServiceServlet;

public class ListSharingInvitationServlet extends WebServiceServlet {

	private static final long serialVersionUID = -6661704993993686163L;

	@Override
	protected WebService getService(Method method) {
		switch (method) {
		case GET:
			return new ListSharingInvitationDetailsService(api);
		case POST:
			return new ListSharingInvitationService(api);
		case PUT:
			return new ListSharingInvitationAcceptanceService(api);
		case DELETE:
			return new ListSharingInvitationDeletionService(api);
		default:
			throw new IllegalStateException("Unknown HTTP method");
		}
	}
}
