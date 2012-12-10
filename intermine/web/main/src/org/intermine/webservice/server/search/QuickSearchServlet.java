package org.intermine.webservice.server.search;

import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.core.NoServiceException;
import org.intermine.webservice.server.core.WebServiceServlet;

public class QuickSearchServlet extends WebServiceServlet
{
    private static final long serialVersionUID = -5506185356973283525L;

    @Override
    protected WebService getService(Method method) throws NoServiceException {
        switch (method) {
            case GET: return new QuickSearch(api);
            case POST: return new QuickSearch(api);
            default: throw new NoServiceException();
        }
    }

}
