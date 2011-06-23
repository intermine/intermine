package org.intermine.webservice.server.lists;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.InterMineAPI;
import org.intermine.web.logic.session.SessionMethods;

public class ListAppendServlet extends HttpServlet {

    /**
     * Constructor.
     */
    public ListAppendServlet() {
        // empty constructor
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        appendToList(request, response);
    }

    private void appendToList(HttpServletRequest request, HttpServletResponse response) {
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        new ListAppendService(im).service(request, response);
    }

}
