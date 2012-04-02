package org.intermine.webservice.server.core;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

public class Request extends HttpServletRequestWrapper
{

    /**
     * Construct a request by wrapping the given request.
     * 
     * @param request
     *            The request from the outside world.
     */
    public Request(HttpServletRequest request) {
        super(request);
    }

    @Override
    public HttpSession getSession() {
        throw new RuntimeException("Web service requests should be stateless.");
    }

    @Override
    public HttpSession getSession(boolean create) {
        throw new RuntimeException("Web service requests should be stateless.");
    }

}
