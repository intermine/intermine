package org.intermine.webservice.server.user;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.InterMineAPI;
import org.intermine.web.context.InterMineContext;

public class WhoAmIServlet extends HttpServlet {

    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
        final InterMineAPI api = InterMineContext.getInterMineAPI();
        new WhoAmIService(api).service(req, resp);
    }

}
