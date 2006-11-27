package org.intermine.web.bag;

/*
 * Copyright (C) 2002-2005 FlyMine
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
import org.intermine.web.Constants;
import org.intermine.web.ForwardParameters;
import org.intermine.web.InterMineAction;
import org.intermine.web.Profile;
import org.intermine.web.ProfileManager;
import org.intermine.web.SessionMethods;

/**
 * @author Xavier Watkins
 *
 */
public class ModifyBagDetailsAction extends InterMineAction
{

    /**
     * Forward to the correct method based on the button pressed
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
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ServletContext servletContext = session.getServletContext();
        ProfileManager pm = (ProfileManager) servletContext.getAttribute(Constants.PROFILE_MANAGER);
        ModifyBagDetailsForm mbdf = (ModifyBagDetailsForm) form;
        if (request.getParameter("removeFromBag") != null) {
            removeFromBag(mbdf.getBagName(), profile, pm, mbdf, session);
        }
        return new ForwardParameters(mapping.findForward("bagDetails"))
                    .addParameter("bagName", mbdf.getBagName()).forward();
    }
    
    /**
     * remove the selected elemments from the bag
    *
    * @param mapping The ActionMapping used to select this instance
    * @param form The optional ActionForm bean for this request (if any)
    * @param request The HTTP request we are processing
    * @param response The HTTP response we are creating
    * @return an ActionForward object defining where control goes next
    * @exception Exception if the application business logic throws
    *  an exception
    */
   private void removeFromBag(String bagName, Profile profile, ProfileManager pm, 
                              ModifyBagDetailsForm mbdf, 
                              HttpSession session)
       throws Exception {
       Map savedBags = (Map) profile.getSavedBags();
       InterMineBag interMineIdBag = (InterMineBag) savedBags.get(bagName);
       for (int i = 0; i < mbdf.getSelectedElements().length; i++) {
           interMineIdBag.removeFromId(new Integer(mbdf.selectedElements[i]));
       }
       SessionMethods.invalidateBagTable(session, bagName);
       profile.saveBag(bagName, interMineIdBag, Constants.MAX_NOT_LOGGED_BAG_SIZE);
       pm.saveProfile(profile);
   }

}
