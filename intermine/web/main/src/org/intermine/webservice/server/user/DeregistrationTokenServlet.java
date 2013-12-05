package org.intermine.webservice.server.user;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.bag.SharedBagManager;
import org.intermine.api.profile.TagManager;
import org.intermine.model.userprofile.SavedBag;
import org.intermine.model.userprofile.SavedTemplateQuery;
import org.intermine.model.userprofile.Tag;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.core.NoServiceException;
import org.intermine.webservice.server.core.WebServiceServlet;

public class DeregistrationTokenServlet  extends WebServiceServlet
{
    /**
     * Generated serial ID.
     */
    private static final long serialVersionUID = -3933431561522570728L;

    @Override
    protected void respond(Method method, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
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
            case POST: return new NewDeletionTokenService(api);
            default: throw new NoServiceException();
        }
    }
    
}
