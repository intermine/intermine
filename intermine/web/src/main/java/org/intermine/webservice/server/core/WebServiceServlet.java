package org.intermine.webservice.server.core;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.InterMineAPI;
import org.intermine.web.context.InterMineContext;
import org.intermine.webservice.server.WebService;

/**
 * A servlet which can be easily configured to define how to route requests, using
 * standard RESTful semantics.
 * @author Alex Kalderimis
 *
 */
public abstract class WebServiceServlet extends HttpServlet
{

    private static final long serialVersionUID = 3419034521176834088L;

    protected final InterMineAPI api;

    public static enum Method {
        /** GET **/
        GET,
        /** POST **/
        POST,
        /** PUT **/
        PUT,
        /** DELETE **/
        DELETE
    };

    /** Constructor **/
    public WebServiceServlet() {
        super();
        api = InterMineContext.getInterMineAPI();
    }

    /**
     * Respond to a request.
     * @param method The current method.
     * @param request The request.
     * @param response The response.
     * @throws ServletException Well it could I suppose.
     * @throws IOException Entirely possible really.
     */
    protected void respond(
            Method method, HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        try {
            WebService service = getService(method);
            // ugly, but better safe than sorry, since null is the bottom type;
            // but strictly speaking, the getService method should throw a
            // NoServiceException instead of returning null.
            if (service == null) {
                throw new NoServiceException();
            }
            service.service(request, response);
        } catch (NoServiceException e) {
            sendNoMethodError(method.toString(), request, response);
        }
    }

    private void sendNoMethodError(String method, HttpServletRequest request,
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

    /**
     * Implement this to route requests.
     * @param method The current method.
     * @return A webservice handler.
     * @throws NoServiceException If no handler matches the method.
     */
    protected abstract WebService getService(Method method) throws NoServiceException;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        String tunnelledMethod = request.getParameter("method");
        if (tunnelledMethod != null && !"".equals(tunnelledMethod.trim())) {
            // This a fake tunnelled request, probably from IE, but possibly json-p
            Method tm;
            try {
                tm = Method.valueOf(tunnelledMethod);
            } catch (IllegalArgumentException e) {
                sendNoMethodError(tunnelledMethod, request, response);
                return;
            }
            respond(tm, request, response);
        } else {
            respond(Method.GET, request, response);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        if ("PUT".equals(request.getParameter("method"))) {
            // This a fake tunnelled request, probably from IE.
            doPut(request, response);
        } else {
            respond(Method.POST, request, response);
        }
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
