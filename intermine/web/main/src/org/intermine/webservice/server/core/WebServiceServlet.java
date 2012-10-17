package org.intermine.webservice.server.core;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.web.context.InterMineContext;
import org.intermine.webservice.server.WebService;
import org.jfree.util.Log;

public abstract class WebServiceServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(WebServiceServlet.class);
    private static final long serialVersionUID = 3419034521176834088L;

    protected final InterMineAPI api;

    public static enum Method { GET, POST, PUT, DELETE };

    public WebServiceServlet() {
        super();
        api = InterMineContext.getInterMineAPI();
    }

    protected void respond(Method method, HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        try {
            WebService service = getService(method);
            // ugly, but better safe than sorry, since null is the bottom type.
            if (service == null) {
                throw new NoServiceException();
            }
            service.service(request, response);
        } catch (NoServiceException e) {
            sendNoMethodError(method, request, response);
        }
    }

    private void sendNoMethodError(Method method, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        // The default no-op servlet behaviour.
        String protocol = request.getProtocol();
        String msg = "The " + method + " method is not supported";
        if (protocol.endsWith("1.1")) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, msg);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
        }
    }

    protected abstract WebService getService(Method method) throws NoServiceException;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        respond(Method.GET, request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        respond(Method.POST, request, response);
    }
    
    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response)
             throws ServletException, IOException {
        respond(Method.PUT, request, response);
    }
    
    @Override
    public void doDelete(HttpServletRequest request, HttpServletResponse response)
             throws ServletException, IOException {
        respond(Method.DELETE, request, response);
    }
}
