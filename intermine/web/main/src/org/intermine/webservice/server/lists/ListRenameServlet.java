package org.intermine.webservice.server.lists;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.InterMineAPI;
import org.intermine.web.logic.session.SessionMethods;

public class ListRenameServlet extends HttpServlet {

    /**
     * Constructor.
     */
    public ListRenameServlet() {
        // empty constructor
    }

    @Override
    public void doGet(HttpServletRequest request, 
        HttpServletResponse response)
        throws ServletException, IOException {
        renameList(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, 
         HttpServletResponse response)
        throws ServletException, IOException {
        renameList(request, response);
    }

    private void renameList(HttpServletRequest request, 
        HttpServletResponse response) {
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        new ListRenameService(im).service(request, response);
    }

}
