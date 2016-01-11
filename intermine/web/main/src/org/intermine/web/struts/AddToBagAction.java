package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.IncompatibleTypesException;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.metadata.Util;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.web.logic.session.SessionMethods;


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

    @Override
    public ActionForward execute(ActionMapping mapping,
            ActionForm form, HttpServletRequest request,
            HttpServletResponse response) {
        int id = Integer.parseInt(request.getParameter("object"));
        HttpSession session = request.getSession();

        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());

        Profile profile = SessionMethods.getProfile(session);
        String bagName = request.getParameter("bag");

        InterMineBag existingBag = profile.getSavedBags().get(bagName);
        if (existingBag != null) {
            // TODO add a warning when object already in bag ??
            try {
                InterMineObject o = im.getObjectStore().getObjectById(id);
                existingBag.addIdToBag(id, Util.getFriendlyName(o.getClass()));
                recordMessage(new ActionMessage("bag.addedToBag", existingBag.getName()),
                        request);
            } catch (IncompatibleTypesException e) {
                recordError(new ActionMessage("bag.typesDontMatch"), request);
            } catch (ObjectStoreException e) {
                recordError(new ActionMessage("bag.error"), request, e);
            }
        } else {
            recordError(new ActionMessage("bag.noSuchBag"), request);
        }
        return mapping.findForward("report");
    }
}
