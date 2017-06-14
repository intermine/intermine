package org.intermine.webservice.server.filter;

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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.intermine.webservice.server.core.SessionlessRequest;

/**
 * Filter to wrap all web service requests in a custom delegating wrapper that
 * prevents sessions being created. Any attempts to create a session on such a wrapped
 * object will cause a runtime exception.
 *
 * @see org.intermine.webservice.server.core.SessionlessRequest
 *
 * @author Alex Kalderimis
 */
public class RequestSubClassFilter implements Filter
{
    @Override
    public void destroy() {
        // Nothing to do...
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        // It only makes sense to use this filter on HTTP requests.
        SessionlessRequest wrapped = new SessionlessRequest((HttpServletRequest) request);
        chain.doFilter(wrapped, response);
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        // Nothing to do...
    }

}
