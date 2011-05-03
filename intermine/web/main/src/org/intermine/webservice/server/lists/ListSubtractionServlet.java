package org.intermine.webservice.server.lists;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.InterMineAPI;
import org.intermine.web.logic.session.SessionMethods;

public class ListSubtractionServlet extends HttpServlet {

    public ListSubtractionServlet() {
        // empty constructor
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        subtractLists(request, response);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        subtractLists(request, response);
    }

    private void subtractLists(HttpServletRequest request, HttpServletResponse response) {
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        new ListSubtractionService(im).service(request, response);
    }


}
