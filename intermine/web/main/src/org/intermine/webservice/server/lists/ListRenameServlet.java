package org.intermine.webservice.server.lists;

import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.core.NoServiceException;
import org.intermine.webservice.server.core.WebServiceServlet;

public class ListRenameServlet extends WebServiceServlet {

    private static final long serialVersionUID = 1890719122988401641L;

    @Override
    protected WebService getService(Method method) throws NoServiceException {
        if (Method.DELETE == method) {
            throw new NoServiceException();
        }
        return new ListRenameService(api);
    }

}
