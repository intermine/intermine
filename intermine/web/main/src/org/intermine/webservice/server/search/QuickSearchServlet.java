package org.intermine.webservice.server.search;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.InterMineAPI;
import org.intermine.web.logic.session.SessionMethods;

public class QuickSearchServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        runService(req, resp);
    }

    /**
     * {@inheritDoc}}
     */
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        runService(req, resp);
    }

    private void runService(HttpServletRequest request, HttpServletResponse response) {
        // To avoid servlet caching always new service is created -->
        // Service has always new data and fields in executor are initialized
        // according new data
        // and not remember fields initialized according previous request data
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        new QuickSearch(im).service(request, response);
    }

}
