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
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.logic.results.RDFObject;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.uri.InterMineLUI;
import org.intermine.web.uri.InterMineLUIConverter;
import org.intermine.web.uri.InvalidPermanentURLException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Filter applied to all the requests endinf with rdf
 *
 * @author Daniela Butano
 */
public class EntityRepresentationFilter implements Filter
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
            String requestURI = request.getRequestURI();
            requestURI = requestURI.substring(0, requestURI.lastIndexOf(".rdf"));

            InterMineLUI lui = new InterMineLUI(requestURI);
            RDFObject rdfObject = new RDFObject(lui, SessionMethods.getInterMineAPI(session),
                    request);
            //manage case when id=-1 -> not found
            if (rdfObject == null) {
                response.setStatus(HttpStatus.SC_NOT_FOUND);
                chain.doFilter(req, res);
            } else {
                ResponseUtil.setRDFXMLContentType(response);
                PrintWriter out = new PrintWriter(response.getOutputStream());
                rdfObject.serializeAsRDF(out);
                response.flushBuffer();
            }
        } catch (InvalidPermanentURLException ex) {
            chain.doFilter(req, res);
        } catch (Exception ex) {
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
