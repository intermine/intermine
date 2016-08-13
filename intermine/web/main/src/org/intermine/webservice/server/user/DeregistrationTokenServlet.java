package org.intermine.webservice.server.user;

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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.core.NoServiceException;
import org.intermine.webservice.server.core.WebServiceServlet;

/**
 * A servlet for routing requests to do with deregistration.
 * @author Alex Kalderimis.
 *
 */
public class DeregistrationTokenServlet  extends WebServiceServlet
{
    /**
     * Generated serial ID.
     */
    private static final long serialVersionUID = -3933431561522570728L;

    @Override
    protected void respond(
            Method method,
            HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        String uid = getUid(request);
        WebService service = null;
        if (Method.GET == method && uid != null) {
            service = getGetter(uid);
        } else if (Method.DELETE == method && uid != null) {
            service = getDeleter(uid);
        }
        if (service != null) {
            service.service(request, response);
            return;
        }
        super.respond(method, request, response);
    }

    private static String getUid(HttpServletRequest request) {
        String pathInfo = StringUtils.defaultString(request.getPathInfo(), "").replaceAll("^/", "");
        if (pathInfo.length() > 0) {
            return pathInfo;
        }
        return null;
    }

    private WebService getGetter(String uid) {
        return new DeletionTokenInfoService(api, uid);
    }

    private WebService getDeleter(String uid) {
        return new DeletionTokenCancellationService(api, uid);
    }

    @Override
    protected WebService getService(Method method) throws NoServiceException {
        switch (method) {
            case POST:
                return new NewDeletionTokenService(api);
            default:
                throw new NoServiceException();
        }
    }

}
