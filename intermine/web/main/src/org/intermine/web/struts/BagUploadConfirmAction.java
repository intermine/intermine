package org.intermine.web.struts;

/* 
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.BagElement;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;

/**
 * Action class for saving a bag from the bagUploadConfirm page into the user profile.
 * @author Kim Rutherford
 */
public class BagUploadConfirmAction extends InterMineAction
{
    /**
     * Action to save a bag from the bagUploadConfirm page into the user profile.
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        if (request.getParameter("goBack") != null) {
            return mapping.findForward("back");
        }
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);

        BagUploadConfirmForm confirmForm = (BagUploadConfirmForm) form;
        String bagName = confirmForm.getBagName();
        if (profile.getSavedBags().get(bagName) != null) {
            recordError(new ActionMessage("errors.savebag.existing"), request);
            return mapping.findForward("error");
        }
        
        String idsString = confirmForm.getMatchIDs().trim();
        String[] ids = StringUtil.split(idsString, " ");

        List contents = new ArrayList();
        
        String bagType = confirmForm.getBagType();
        for (int i = 0; i < ids.length; i++) {
            String idString = ids[i];
            if (idString.length() == 0) {
                continue;
            }
            int id = Integer.parseInt(idString);
            contents.add(new BagElement(new Integer(id), bagType));
        }
        for (int i = 0; i < confirmForm.getSelectedObjects().length; i++) {
            String idString = confirmForm.getSelectedObjects()[i];
            int id = Integer.parseInt(idString);
            contents.add(new BagElement(new Integer(id), bagType));
        }
        
        if (contents.size() == 0) {
            recordError(new ActionMessage("bagUploadConfirm.emptyBag"), request);
            return mapping.findForward("error");
        }
        
        ProfileManager profileManager =
            (ProfileManager) servletContext.getAttribute(Constants.PROFILE_MANAGER);
        ObjectStore profileOs = profileManager.getUserProfileObjectStore();
                                                                      
        InterMineBag bag =
            new InterMineBag(profile.getUserId(), bagName, bagType, profileOs, os, contents);
        
        profile.saveBag(bagName, bag, -1);

        session.removeAttribute("bagQueryResult");
        
        ForwardParameters forwardParameters = 
            new ForwardParameters(mapping.findForward("bagDetails"));
        return forwardParameters.addParameter("bagName", bagName).forward();
    }
}
