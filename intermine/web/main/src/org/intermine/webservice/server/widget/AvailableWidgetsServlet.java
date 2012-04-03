package org.intermine.webservice.server.widget;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.InterMineAPI;
import org.intermine.web.context.InterMineContext;

public class AvailableWidgetsServlet extends HttpServlet {

    /**
     * Generated serial id.
     */
    private static final long serialVersionUID = 4536224836847168699L;

    /**
     * {@inheritDoc}}
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
                    IOException {
        runService(req, resp);
    }

    private void runService(HttpServletRequest request, HttpServletResponse response) {
        // To avoid servlet caching always new service is created -->
        // Service has always new data and fields in executor are initialized
        // according new data
        // and not remember fields initialized according previous request data
        final InterMineAPI im = InterMineContext.getInterMineAPI();
        new AvailableWidgetsService(im).service(request, response);
    }

}
