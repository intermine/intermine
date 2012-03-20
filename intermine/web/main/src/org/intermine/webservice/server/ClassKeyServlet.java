package org.intermine.webservice.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.InterMineAPI;
import org.intermine.web.logic.session.SessionMethods;

public class ClassKeyServlet extends HttpServlet {

    private static final long serialVersionUID = -8916814874009422133L;

    /**
     * {@inheritDoc}}
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        runService(request, response);
    }

    private void runService(HttpServletRequest request, HttpServletResponse response) {
        InterMineAPI im = SessionMethods.getInterMineAPI(request);
        WebService s = new ClassKeysService(im);
        s.service(request, response);
    }
}
