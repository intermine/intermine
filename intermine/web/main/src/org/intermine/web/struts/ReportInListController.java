package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.web.logic.results.ReportInList;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;

/**
 * @author "Xavier Watkins"
 *
 */
public class ReportInListController extends TilesAction
{
   /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        ReportObject object = (ReportObject) context.getAttribute("object");
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Profile profile = SessionMethods.getProfile(session);
        BagManager bagManager = im.getBagManager();

        Collection<InterMineBag> bagsWithId =
            bagManager.getCurrentUserOrGlobalBagsContainingId(profile, object.getId());
        // wrap around
        ReportInList odil = new ReportInList(bagsWithId);

        request.setAttribute("bagsWithId", odil);

        List<InterMineBag> bagsToAddTo = new ArrayList<InterMineBag>();
        Map<String, InterMineBag> userBags =
            bagManager.getCurrentUserBagsOfType(profile, object.getType());
        for (String bagName : userBags.keySet()) {
            if (!bagsWithId.contains(userBags.get(bagName))) {
                bagsToAddTo.add(userBags.get(bagName));
            }
        }
        request.setAttribute("bagsToAddTo", bagsToAddTo);
        return null;
    }
}
