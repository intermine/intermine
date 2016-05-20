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
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.bag.BagQueryUpgrade;
import org.intermine.api.idresolution.IDResolver;
import org.intermine.api.idresolution.Job;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.web.logic.session.SessionMethods;


/**
 * An action that prepare a BagQueryResult and save it into the session.
 *
 * @author Daniela Butano
 */

public class BagUpgradeAction extends InterMineAction
{

    private static final String WS_JOB_ID_KEY = "idresolutionjobid";

    /**
     * Action for creating BagQueryResult for a specific bag not yet current
     *
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
            HttpServletResponse response) throws Exception {
        String bagName = (String) request.getParameter("bagName");
        HttpSession session = request.getSession();

        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Profile profile = SessionMethods.getProfile(session);
        InterMineBag savedBag = profile.getSavedBags().get(bagName);

        BagQueryRunner bagRunner = im.getBagQueryRunner();
        BagQueryUpgrade bagQueryUpgrade = new BagQueryUpgrade(bagRunner, savedBag);
        Job job = IDResolver.getInstance().submit(bagQueryUpgrade);
        session.setAttribute(WS_JOB_ID_KEY, job.getUid());

        request.setAttribute("newBagName", bagName);
        request.setAttribute("bagType", savedBag.getType());
        return mapping.findForward("bagUploadConfirm");
    }
}
