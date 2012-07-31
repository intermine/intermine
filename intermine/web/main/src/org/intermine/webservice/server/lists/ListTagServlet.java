package org.intermine.webservice.server.lists;

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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.InterMineAPI;
import org.intermine.web.context.InterMineContext;
import org.intermine.webservice.server.WebService;

/**
 * A servlet for routing list tag requests based on HTTP method.
 * @author Alex Kalderimis
 *
 */
public class ListTagServlet extends HttpServlet
{

    /**
     * Generated serial id.
     */
    private static final long serialVersionUID = -3135896734821503223L;


    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        addTags(request, response);
    }

    @Override
    public void doDelete(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        removeTags(request, response);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        getTags(request, response);
    }

    // Private Methods

    private void removeTags(HttpServletRequest request,
            HttpServletResponse response) {
        final InterMineAPI im = InterMineContext.getInterMineAPI();
        WebService tagService = new ListTagRemovalService(im);
        tagService.service(request, response);
    }

    private void addTags(HttpServletRequest request,
            HttpServletResponse response) {
        final InterMineAPI im = InterMineContext.getInterMineAPI();
        WebService tagService = new ListTagAddingService(im);
        tagService.service(request, response);
    }

    private void getTags(HttpServletRequest request, HttpServletResponse response) {
        final InterMineAPI im = InterMineContext.getInterMineAPI();
        WebService tagService = new ListTagService(im);
        tagService.service(request, response);
    }

}
