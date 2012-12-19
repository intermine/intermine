package org.intermine.webservice.server.idresolution;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.core.NoServiceException;
import org.intermine.webservice.server.core.WebServiceServlet;

public class IdResolutionServlet extends WebServiceServlet
{
    private static final long serialVersionUID = -3364780354450369691L;

    @Override
    protected void respond(Method method, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (Method.GET == method) {
            String[] uidAndCommand = getUidAndCommand(request);
            if (uidAndCommand != null) {
                WebService getter = getGetter(uidAndCommand[0], uidAndCommand[1]);
                if (getter != null) {
                    getter.service(request, response);
                    return;
                }
            }
        }
        super.respond(method, request, response);
    }

    private static String[] getUidAndCommand(HttpServletRequest request) {
        String pathInfo = StringUtils.defaultString(request.getPathInfo(), "").replaceAll("^/", "");
        int slashIndex = pathInfo.indexOf('/');
        if (slashIndex > 0) {
            String jobId = pathInfo.substring(0, slashIndex);
            String command = pathInfo.substring(slashIndex + 1);
            return new String[] {jobId, command};
        }
        return null;
    }

    private WebService getGetter(String uid, String command) {
        WebService ws = null;
        if ("status".equalsIgnoreCase(command)) {
            ws = new JobStatusService(api, uid);
        } else if ("result".equalsIgnoreCase(command) || "results".equalsIgnoreCase(command)) {
            ws = new JobResultsService(api, uid);
        }
        return ws;
    }

    @Override
    protected WebService getService(Method method) throws NoServiceException {
        switch (method) {
            case POST: return new IdResolutionService(api);
            default: throw new NoServiceException();
        }
    }
}
