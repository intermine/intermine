package org.intermine.webservice.server;

import org.intermine.webservice.server.core.NoServiceException;
import org.intermine.webservice.server.core.WebServiceServlet;

import javax.servlet.ServletContext;


public class FacetServlet extends WebServiceServlet {
    @Override
    protected WebService getService(Method method) throws NoServiceException {
        ServletContext ctx = this.getServletContext();
        switch (method) {
            case GET:
                return new FacetService(api);
            case POST:
                return new FacetService(api);
            default:
                throw new NoServiceException();

        }
    }
}
