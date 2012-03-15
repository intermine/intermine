package org.intermine.bio.webservice;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.web.logic.session.SessionMethods;

/**
 * A servlet to hand off to the GenomicRegionSequence query service.
 *
 * @author Fengyuan Hu
 *
 */
public class GenomicRegionSequenceExportServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(GenomicRegionSequenceExportServlet.class);

    /**
     * {@inheritDoc}}
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        runService(request, response);
    }

    /**
     * {@inheritDoc}}
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        runService(req, resp);
    }

    private void runService(HttpServletRequest request,
            HttpServletResponse response) {
        // To avoid servlet caching always new service is created -->
        // Service has always new data and fields in executor are initialized
        // according new data
        // and not remember fields initialized according previous request data
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        new GenomicRegionSequenceExportService(im).service(request, response);
    }
}
