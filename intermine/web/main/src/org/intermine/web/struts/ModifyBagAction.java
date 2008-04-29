package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.query.ObjectStoreBagCombination;

import org.intermine.web.logic.BagUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Implementation of <strong>Action</strong> to modify bags
 * @author Mark Woodbridge
 */
public class ModifyBagAction extends InterMineAction
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
                                 @SuppressWarnings("unused") HttpServletResponse response)
    throws Exception {
        ModifyBagForm mbf = (ModifyBagForm) form;

        if (request.getParameter("union") != null
                        || (mbf.getListsButton() != null && mbf.getListsButton().equals("union"))) {
            BagUtil.combine(mapping, form, request, ObjectStoreBagCombination.UNION, "UNION");
        } else if (request.getParameter("intersect") != null
                        || (mbf.getListsButton() != null
                                        && mbf.getListsButton().equals("intersect"))) {
            BagUtil.combine(mapping, form, request, ObjectStoreBagCombination.INTERSECT, 
                            "INTERSECT");
        } else if (request.getParameter("subtract") != null
                        || (mbf.getListsButton() != null
                                        && mbf.getListsButton().equals("subtract"))) {
            BagUtil.combine(mapping, form, request, ObjectStoreBagCombination.ALLBUTINTERSECT,
            "SUBTRACT");
        } else if (request.getParameter("delete") != null
                        || (mbf.getListsButton() != null
                                        && mbf.getListsButton().equals("delete"))) {
            delete(mapping, form, request);
        }

        return BagUtil.getReturn(mbf.getPageName(), mapping);
    }

    /**
     * Delete the selected bags
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward delete(ActionMapping mapping,
                                ActionForm form,
                                HttpServletRequest request)
        throws Exception {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);

        ModifyBagForm mbf = (ModifyBagForm) form;
        for (int i = 0; i < mbf.getSelectedBags().length; i++) {
            InterMineBag bag = profile.getSavedBags().get(mbf.getSelectedBags()[i]);
            BagUtil.deleteBag(session, profile, bag);
        }

        return BagUtil.getReturn(mbf.getPageName(), mapping);
    }




}
