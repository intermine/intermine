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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.BagHelper;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;

/**
 * Action to save a single object o an existing bag.
 * @author Thomas Riley
 */
public class AddToBagAction extends InterMineAction
{
    /**
     * Save a single object to an existing bag.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     */
    public ActionForward execute(ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response) {
        int id = Integer.parseInt(request.getParameter("object"));
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ObjectStoreWriter uosw = ((ProfileManager) servletContext.getAttribute(Constants
                    .PROFILE_MANAGER)).getUserProfileObjectStore();
        String bagName = request.getParameter("bag");
        
        InterMineBag existingBag = (InterMineBag) profile.getSavedBags().get(bagName);
        if (existingBag != null) {
            ObjectStoreWriter osw = null;
            try {
                InterMineObject o = (InterMineObject) os.getObjectById(new Integer(id));
                if (BagHelper.isOfBagType(existingBag, o, os)) {
                    osw = new ObjectStoreWriterInterMineImpl(os);
                    osw.addToBag(existingBag.getOsb(), new Integer(id));
                    recordMessage(new ActionMessage("bag.addedToBag", existingBag.getName())
                                    , request);
                } else {
                    recordError(new ActionMessage("bag.typesDontMatch"), request);
                }
                // TODO add a warning when object already in bag ??
            } catch (ObjectStoreException e) {
                recordError(new ActionMessage("bag.error"), request, e);

                return mapping.findForward("objectDetails");
            } finally {
                try {
                    if (osw != null) {
                        osw.close();
                    }
                } catch (ObjectStoreException e) {
                }
            }
        } else {
            recordError(new ActionMessage("bag.noSuchBag"), request);
        }
        return mapping.findForward("objectDetails");
    }
}
