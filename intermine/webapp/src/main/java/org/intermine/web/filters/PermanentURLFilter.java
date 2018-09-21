package org.intermine.web.filters;

/*
 * Copyright (C) 2002-2018 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import org.intermine.api.url.*;
import org.intermine.objectstore.ObjectStoreException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class PermanentURLFilter implements Filter
{
    private static final Logger LOGGER = Logger.getLogger(PermanentURLFilter.class);

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        try {
            PermanentURL permanentURL = new PermanentURL(request.getRequestURI());
            URLConverter urlConverter = new URLConverter();
            Integer id = urlConverter.getIntermineID(permanentURL);
            if (id == -1) {
                response.setStatus(HttpStatus.SC_NOT_FOUND);
                chain.doFilter(req, res);
            } else {
                String redirectURL = request.getContextPath() + "/report.do?id=" + id;
                response.sendRedirect(redirectURL);
            }
        } catch (InvalidPermanentURLException ex) {
            chain.doFilter(req, res);
        } catch (ObjectStoreException ex) {
            response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
        return;
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