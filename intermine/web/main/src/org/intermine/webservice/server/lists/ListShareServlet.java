package org.intermine.webservice.server.lists;

import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.core.NoServiceException;
import org.intermine.webservice.server.core.WebServiceServlet;

public class ListShareServlet extends WebServiceServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected WebService getService(Method method) throws NoServiceException {
		switch (method) {
		case GET:
			return new ListShareDetailsService(api);
		case POST:
			return new ListShareCreationService(api);
		case DELETE:
			return new ListShareDeletionService(api);
		default:
			throw new NoServiceException();
		}
	}

}
