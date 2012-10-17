package org.intermine.webservice.server.lists;

import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.core.NoServiceException;
import org.intermine.webservice.server.core.WebServiceServlet;

public class AvailableListsServlet extends WebServiceServlet
{
    private static final long serialVersionUID = 1L;

    @Override
    protected WebService getService(Method method) throws NoServiceException {
        switch (method) {
        case GET:
            return new AvailableListsService(api);
        case POST:
            return new ListUploadService(api);
        case DELETE:
            return new ListDeletionService(api);
        default:
            throw new NoServiceException();
        }
    }

}
