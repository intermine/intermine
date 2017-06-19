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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

/** @author Alex Kalderimis **/
public class SessionlessRequest extends HttpServletRequestWrapper
{

    /**
     * Construct a request by wrapping the given request.
     *
     * @param request
     *            The request from the outside world.
     */
    public SessionlessRequest(HttpServletRequest request) {
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
