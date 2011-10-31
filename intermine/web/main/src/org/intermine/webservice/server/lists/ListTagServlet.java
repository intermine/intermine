package org.intermine.webservice.server.lists;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.InterMineAPI;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.server.WebService;

public class ListTagServlet extends HttpServlet {

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
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        WebService tagService = new ListTagRemovalService(im);
        tagService.service(request, response);
    }

    private void addTags(HttpServletRequest request,
            HttpServletResponse response) {
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        WebService tagService = new ListTagAddingService(im);
        tagService.service(request, response);
    }

    private void getTags(HttpServletRequest request, HttpServletResponse response) {
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        WebService tagService = new ListTagService(im);
        tagService.service(request, response);
    }

}
