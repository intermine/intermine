package org.intermine.web.filters;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import javax.servlet.http.HttpServletResponse;

/**
 * A Filter that modifies response headers of outgoing pages.
 * @author Richard Smith
 */
public class ExpiresHeaderFilter implements Filter
{
    static final int CACHE_DURATION_IN_SECOND = 60 * 60 * 24 * 2; // 2 days
    static final long   CACHE_DURATION_IN_MS = CACHE_DURATION_IN_SECOND  * 1000;

    /**
     * Add an expires header to matching files to help browsers avoid reloading static files.
     * {@inheritDoc}
     */
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
        throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;

        long now = System.currentTimeMillis();
        //res being the HttpServletResponse of the request
        response.addHeader("Cache-Control", "max-age=" + CACHE_DURATION_IN_SECOND);
        response.addHeader("Cache-Control", "must-revalidate"); // optional
        response.setDateHeader("Last-Modified", now);
        response.setDateHeader("Expires", now + CACHE_DURATION_IN_MS);

        chain.doFilter(req, response);
    }

    /**
     * Initialise this Filter.
     * {@inheritDoc}
     */
    public void init(FilterConfig filterConfig) {
       // empty
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() {
       // empty
    }
}
