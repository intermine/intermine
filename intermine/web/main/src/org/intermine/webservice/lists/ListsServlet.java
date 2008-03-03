package org.intermine.webservice.lists;

/*
 * Copyright (C) 2002-2008 FlyMine
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

/**
 * Servlet that runs ListsService web service.
 * @see org.intermine.webservice.lists.ListsService for more information. 
 * @author Jakub Kulaviak 
 **/
public class ListsServlet extends HttpServlet
{
    /**
     * Default serialization id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}}
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        new ListsService().doGet(request, response);
    }
}
