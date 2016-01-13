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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.tracker.util.ListBuildMode;
import org.intermine.metadata.StringUtil;
import org.intermine.web.logic.session.SessionMethods;

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
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        if (request.getParameter("goBack") != null) {
            return mapping.findForward("back");
        }
        HttpSession session = request.getSession();
        Profile profile = SessionMethods.getProfile(session);
        InterMineAPI im = SessionMethods.getInterMineAPI(session);

        BagUploadConfirmForm confirmForm = (BagUploadConfirmForm) form;
        String bagName = (!"".equals(confirmForm.getNewBagName()))
                         ? confirmForm.getNewBagName()
                         : request.getParameter("upgradeBagName");
        if (StringUtils.isBlank(bagName)) {
            recordError(new ActionMessage("bagUploadConfirm.noName"), request);
            return mapping.findForward("error");
        }

        String idsString = confirmForm.getMatchIDs().trim();
        String[] ids = StringUtil.split(idsString, " ");

        List<Integer> contents = new ArrayList<Integer>();

        String bagType = confirmForm.getBagType();
        for (int i = 0; i < ids.length; i++) {
            String idString = ids[i];
            if (idString.length() == 0) {
                continue;
            }
            int id = Integer.parseInt(idString);
            contents.add(new Integer(id));
        }
        for (int i = 0; i < confirmForm.getSelectedObjects().length; i++) {
            String idString = confirmForm.getSelectedObjects()[i];
            int id = Integer.parseInt(idString);
            contents.add(new Integer(id));
        }

        if (contents.size() == 0) {
            recordError(new ActionMessage("bagUploadConfirm.emptyBag"), request);
            return mapping.findForward("error");
        }

        // if upgradeBagName is null we are creating a new bag,
        // otherwise we are upgrading an existing bag
        if (request.getParameter("upgradeBagName") == null) {
            InterMineBag bag = profile.createBag(bagName, bagType, "", im.getClassKeys());
            bag.addIdsToBag(contents, bagType);
            //track the list creation
            im.getTrackerDelegate().trackListCreation(bagType, bag.getSize(),
                    ListBuildMode.IDENTIFIERS, profile, session.getId());
            session.removeAttribute("bagQueryResult");
        } else {
            InterMineBag bagToUpgrade = profile.getSavedBags().get(bagName);
            if (bagToUpgrade == null) {
                recordError(new ActionMessage("bagUploadConfirm.notFound"), request);
                return mapping.findForward("error");
            }
            bagToUpgrade.upgradeOsb(contents, true);
            session.removeAttribute("bagQueryResult_" + bagName);
        }
        confirmForm.reset(mapping, request);

        ForwardParameters forwardParameters = new ForwardParameters(
                mapping.findForward("bagDetails"));
        forwardParameters.addParameter("bagName", bagName);
        forwardParameters.addParameter("trackExecution", "false");
        return forwardParameters.forward();
    }
}
