package org.intermine.webservice.server.user;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.InterMineAPI;
import org.intermine.web.context.InterMineContext;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.core.NoServiceException;
import org.intermine.webservice.server.core.WebServiceServlet;

public class WhoAmIServlet extends WebServiceServlet {

    public WebService getService(Method method) throws NoServiceException {
        if (Method.GET == method) {
            return new WhoAmIService(api);
        } else {
            throw new NoServiceException();
        }
    }

}
