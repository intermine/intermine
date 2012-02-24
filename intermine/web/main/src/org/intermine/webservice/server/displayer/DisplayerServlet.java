package org.intermine.webservice.server.displayer;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.InterMineAPI;
import org.intermine.web.logic.session.SessionMethods;

public class DisplayerServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        final InterMineAPI im = SessionMethods.getInterMineAPI(request);
        new DisplayerService(im).service(request, response);
    }
}
