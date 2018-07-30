package org.intermine.webservice.server;

import org.intermine.webservice.server.core.NoServiceException;
import org.intermine.webservice.server.core.WebServiceServlet;

import javax.servlet.ServletContext;

public class FacetListServlet extends WebServiceServlet {
    @Override
    protected WebService getService(Method method) throws NoServiceException {
        ServletContext ctx = this.getServletContext();
        switch (method) {
            case GET:
                return new FacetListService(api);
            case POST:
                return new FacetListService(api);
            default:
                throw new NoServiceException();

        }
    }
}
