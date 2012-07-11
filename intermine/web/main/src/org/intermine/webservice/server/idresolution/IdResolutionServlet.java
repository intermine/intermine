package org.intermine.webservice.server.idresolution;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.web.context.InterMineContext;
import org.intermine.webservice.server.WebService;

public class IdResolutionServlet extends HttpServlet
{

    Logger log = Logger.getLogger(IdResolutionServlet.class);
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException {
        String pathInfo = request.getPathInfo().replaceAll("^/", "");
        if (pathInfo == null || pathInfo.length() == 0) {
            throw new ServletException("no job id");
        }
        WebService ws;
        log.info(pathInfo);
        int slashIndex = pathInfo.indexOf('/');
        // Either called as GET /ids/UID/status or GET /ids/UID/result
        if (slashIndex > 0) {
            InterMineAPI im = InterMineContext.getInterMineAPI();
            String jobId = pathInfo.substring(0, slashIndex);
            String command = pathInfo.substring(slashIndex + 1);
            log.info(command + " of " + jobId);
            if ("status".equalsIgnoreCase(command)) {
                ws = new JobStatusService(im, jobId);
            } else if ("result".equalsIgnoreCase(command)) {
                ws = new JobResultsService(im, jobId);
            } else {
                throw new ServletException("bad command: " + command);
            }
        } else {
            throw new ServletException("No command");
        }

        ws.service(request, response);
    }
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        new IdResolutionService(InterMineContext.getInterMineAPI()).service(request, response);
    }

}
