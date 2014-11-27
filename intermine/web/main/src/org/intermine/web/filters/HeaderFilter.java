package org.intermine.web.filters;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * A Filter that modifies response headers of outgoing pages.
 * @author Kim Rutherford
 */
public class HeaderFilter implements Filter
{
    private FilterConfig fc;

    /**
     * Do the filtering.
     * {@inheritDoc}
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
        throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;

        @SuppressWarnings("unchecked")
        Enumeration<String> e = fc.getInitParameterNames();
        while (e.hasMoreElements()) {
            String headerName = e.nextElement();
            String headerValue = fc.getInitParameter(headerName);

            response.addHeader(headerName, headerValue);
        }

        chain.doFilter(req, response);
    }

    /**
     * Initialise this Filter.
     * {@inheritDoc}
     */
    @Override
    public void init(FilterConfig filterConfig) {
        this.fc = filterConfig;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
       // empty
    }
}
