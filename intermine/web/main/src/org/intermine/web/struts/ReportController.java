package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.InterMineAPI;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.web.logic.results.DisplayObject;
import org.intermine.web.logic.results.DisplayObjectFactory;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.results.ReportObjectFactory;
import org.intermine.web.logic.session.SessionMethods;

/**
 * New objectDetails.
 *
 * @author Radek Stepan
 */
public class ReportController extends InterMineAction
{
    private InterMineObject requestedObject;
    private ReportObject reportObject;

    private HttpSession session;

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(@SuppressWarnings("unused") ActionMapping mapping,
            @SuppressWarnings("unused") ActionForm form, HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {

            // fetch & set requested object
            if (isRequestedObjectValid(request)) {
                ReportObjectFactory reportObjectFactory = SessionMethods.getReportObjects(session);
                reportObject = reportObjectFactory.get(requestedObject);
                request.setAttribute("reportObject", reportObject);

                request.setAttribute("requestedObject", requestedObject);
            }

            return null;
    }

    /**
     * Setup session, API, ObjectStore and set object requested
     * @param request
     * @return true if object is setup OK
     */
    private boolean isRequestedObjectValid(HttpServletRequest request) {
        // get parameter
        String idString = request.getParameter("id");
        // parse to integer
        Integer id = new Integer(Integer.parseInt(idString));
        // get session
        session = request.getSession();
        // get API
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        // get ObjectStore
        ObjectStore os = im.getObjectStore();
        // set object
        try {
            requestedObject = os.getObjectById(id);
        } catch (ObjectStoreException e) {
            e.printStackTrace();
        }
        return requestedObject != null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getClassDescriptions() {
        // assert session needs to be set already!
        ServletContext servletContext = session.getServletContext();
        return (Map<String, String>) servletContext.getAttribute("classDescriptions");
    }

}
