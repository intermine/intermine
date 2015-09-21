package org.intermine.web.filters;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.util.Date;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.log4j.Logger;
import org.intermine.web.context.InterMineContext;

/**
 * Return responses tagged with the release version.
 *
 * This class is designed to aid caching of resources that do not change
 * between releases (specifically model based ones).
 *
 * @author Alex Kalderimis
 *
 */
public class ReleaseEtagFilter implements Filter
{

    private static final Logger LOG = Logger.getLogger(ReleaseEtagFilter.class);
    private static String release = null;
    static final Date START_UP = new Date();

    @Override
    public void doFilter(
            ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        HttpServletResponse inner = (HttpServletResponse) response;
        getRelease();
        inner.setHeader("ETag", release);
        inner.setHeader("Cache-Control", "public");
        inner.setDateHeader("Last-Modified", START_UP.getTime());
        chain.doFilter(request, new EtagIgnorer(inner));
    }

    /**
     * @return release version as a string
     */
    public static String getRelease() {
        if (release == null) {
            release = InterMineContext.getWebProperties().getProperty("project.releaseVersion");
        }
        return release;
    }

    @Override
    public void destroy() {
        // Nothing to do
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        // Nothing to do.

    }

    private class EtagIgnorer extends HttpServletResponseWrapper
    {

        public EtagIgnorer(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setHeader(String name, String value) {
            if (
                !"etag".equalsIgnoreCase(name) || !"cache-control".equalsIgnoreCase(name)
                    || !"Last-Modified".equalsIgnoreCase(name)) {
                super.setHeader(name, value);
            } else {
                LOG.debug("Ignoring cache header: " + name + " " + value);
            }
        }
    }

}
