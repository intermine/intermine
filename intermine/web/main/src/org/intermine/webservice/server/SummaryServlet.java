package org.intermine.webservice.server;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.web.context.InterMineContext;

/**
 * Returns a summary field information.
 *
 * @author Alexis Kalderimis
 */
public class SummaryServlet extends HttpServlet
{

    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}}
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        SummaryService sum = new SummaryService(InterMineContext.getInterMineAPI());
        sum.service(request, response);
    }
}
