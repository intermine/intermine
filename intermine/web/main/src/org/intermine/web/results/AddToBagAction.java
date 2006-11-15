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

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.InterMineException;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.Constants;
import org.intermine.web.InterMineAction;
import org.intermine.web.Profile;
import org.intermine.web.SessionMethods;
import org.intermine.web.WebUtil;
import org.intermine.web.bag.InterMineBag;
import org.intermine.web.bag.InterMineIdBag;
import org.intermine.web.bag.InterMinePrimitiveBag;

/**
 * 
 * @author Thomas Riley
 */
public class AddToBagAction extends InterMineAction
{
    /**
     * Save a single object o an existing bag.
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        int id = Integer.parseInt(request.getParameter("object"));
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        String bagName = request.getParameter("bag");
        
        InterMineBag existingBag = (InterMineBag) profile.getSavedBags().get(bagName);
        if (existingBag != null) {
            if (existingBag instanceof InterMinePrimitiveBag) {
                // error
                recordError(new ActionMessage("bag.typesDontMatch"), request);
            } else {
                ((InterMineIdBag) existingBag).add(id);
                try {
                    int maxNotLoggedSize = WebUtil.getIntSessionProperty(session,
                                                  "max.bag.size.notloggedin",
                                                  Constants.MAX_NOT_LOGGED_BAG_SIZE);
                    profile.saveBag(bagName, existingBag, maxNotLoggedSize);
                } catch (InterMineException e) {
                    recordError(new ActionMessage(e.getMessage()), request);
                }
                SessionMethods.invalidateBagTable(session, bagName);
                recordMessage(new ActionMessage("bag.addedToBag", bagName), request);
                return mapping.findForward("objectDetails");
            }
        } else {
            recordError(new ActionMessage("bag.noSuchBag"), request);
        }
        return mapping.findForward("objectDetails");
    }
}
