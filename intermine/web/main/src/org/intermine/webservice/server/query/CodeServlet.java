package org.intermine.webservice.server.query;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.InterMineAPI;
import org.intermine.web.logic.session.SessionMethods;

public class CodeServlet extends HttpServlet {

    /**
     * Eclipse made me do it!!
     */
    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        runService(request, response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        runService(request, response);
    }

    private void runService(HttpServletRequest request, HttpServletResponse response) {
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        new CodeService(im).service(request, response);
    }
}
