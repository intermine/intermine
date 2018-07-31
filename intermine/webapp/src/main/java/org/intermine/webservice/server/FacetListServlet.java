package org.intermine.webservice.server;

import org.intermine.web.context.InterMineContext;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FacetListServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        FacetListService facetListService = new FacetListService(InterMineContext.getInterMineAPI());
        facetListService.service(request, response);
    }
}
