package org.intermine.webservice.server.user;

import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.core.NoServiceException;
import org.intermine.webservice.server.core.WebServiceServlet;

public class PreferencesServlet extends WebServiceServlet {

    private static final long serialVersionUID = -1345782823632940170L;

    @Override
    protected WebService getService(Method method) throws NoServiceException {
        switch (method) {
        case GET:
            return new ReadPreferencesService(api);
        case POST:
            return new SetPreferencesService(api);
        case PUT:
            return new SetPreferencesService(api);
        case DELETE:
            return new DeletePreferencesService(api);
        default:
            throw new NoServiceException();
        }
    }

}
