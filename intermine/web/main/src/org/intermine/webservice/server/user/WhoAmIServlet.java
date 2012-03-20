package org.intermine.webservice.server.user;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.web.logic.session.SessionMethods;

public class WhoAmIServlet extends HttpServlet {

    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
        final InterMineAPI api = SessionMethods.getInterMineAPI(req);
        new WhoAmIService(api).service(req, resp);
    }

}
