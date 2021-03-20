package org.intermine.web.filters;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.commons.httpclient.HttpStatus;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.uri.InterMineLUI;
import org.intermine.web.uri.InterMineLUIConverter;
import org.intermine.web.uri.InvalidPermanentURLException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Filter applied to all the requests to the InterMine server in order
 * to catch the requests with permanent URL and redirect to the report page.
 * Example: humanmine.org/humanmine/protein:P31946 -> humanmine.org/humanmine/report.do?id=1234567
 *
 * @author danielabutano
 */
public class PermanentURLFilter implements Filter
{
    /**
     * Filters all the intermine requests
     * {@inheritDoc}
     */
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        HttpSession session = request.getSession();

        try {
            InterMineLUI permanentURL = new InterMineLUI(request.getRequestURI());
            InterMineLUIConverter urlConverter =
                    new InterMineLUIConverter(SessionMethods.getProfile(session));
            Integer id = urlConverter.getInterMineID(permanentURL);
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
