package org.intermine.webservice.server.idresolution;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.core.NoServiceException;
import org.intermine.webservice.server.core.WebServiceServlet;

/**
 * Route requests for ID resolution.
 * @author Alex Kalderimis
 *
 */
public class IdResolutionServlet extends WebServiceServlet
{
    private static final Logger LOG = Logger.getLogger(IdResolutionServlet.class);
    private static final long serialVersionUID = -3364780354450369691L;
    private JobJanitor janitor = null;
    private Thread janitorThread = null;

    @Override
    public void init(ServletConfig config) {
        try {
            janitor = new JobJanitor();
            janitorThread = new Thread(janitor);
            janitorThread.setDaemon(true);
            janitorThread.start();
        } catch (Exception e) {
            LOG.warn(e);
        }
    }

    @Override
    public void destroy() {
        if (janitor != null) {
            janitor.stop();
        }
        if (janitorThread != null) {
            janitorThread.interrupt();
        }
        super.destroy();
    }

    @Override
    protected void respond(
            Method method,
            HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
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
            case POST:
                return new IdResolutionService(api);
            case DELETE:
                return new JobRemovalService(api);
            default:
                throw new NoServiceException();
        }
    }
}
