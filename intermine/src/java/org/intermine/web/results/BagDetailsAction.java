package org.intermine.web.results;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.Constants;
import org.intermine.web.ForwardParameters;
import org.intermine.web.InterMineBag;
import org.intermine.web.Profile;
import org.intermine.web.SessionMethods;

/**
 * Action that builds a PagedCollection to view a bag. Redirects to results.do
 *
 * @author Kim Rutherford
 * @author Thomas Riley
 */
public class BagDetailsAction extends Action
{
    /**
     * Set up session attributes for the bag details page.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if an error occurs
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        
        String bagName = request.getParameter("bagName");
        InterMineBag bag = (InterMineBag) profile.getSavedBags().get(bagName);

        if (bag == null) {
            bag = new InterMineBag(os);
        }
        
        String identifier = "bag." + bagName;
        PagedCollection pc = (PagedCollection) SessionMethods.getResultsTable(session, identifier);
        if (pc == null) {
            pc = new PagedCollection(bagName, bag, Object.class);
            SessionMethods.setResultsTable(session, identifier, pc);
        }
        
        return new ForwardParameters(mapping.findForward("results"))
                        .addParameter("table", identifier)
                        .addParameter("size", "25").forward();
    }
}
