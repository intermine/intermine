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


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.UnknownBagTypeException;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.tracker.util.ListBuildMode;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Saves selected items in a new bag or combines with existing bag.
 *
 * @author Andrew Varley
 * @author Thomas Riley
 * @author Kim Rutherford
 */
public class SaveBagAction extends InterMineAction
{
    protected static final Logger LOG = Logger.getLogger(SaveBagAction.class);

    /**
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    @Override
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        return saveBag(mapping, form, request, response);
    }

    /**
     * The batch size to use when we need to iterate through the whole result set.
     */
    public static final int BIG_BATCH_SIZE = 10000;

    /**
     * Save the selected objects to a bag on the session
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     */
    public ActionForward saveBag(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response) {
        HttpSession session = request.getSession();
        Profile profile = SessionMethods.getProfile(session);
        PagedTable pt = SessionMethods.getResultsTable(session, request.getParameter("table"));
        SaveBagForm sbf = (SaveBagForm) form;

        String bagName = null;
        String operation = "";

        if (request.getParameter("saveNewBag") != null
                        || (sbf.getOperationButton() != null
                        && "saveNewBag".equals(sbf.getOperationButton()))) {
            bagName = sbf.getNewBagName();
            operation = "saveNewBag";
        } else {
            bagName = sbf.getExistingBagName();
            operation = "addToBag";
        }

        if (bagName == null) {
            return null;
        }

        if (pt.isEmptySelection()) {
            ActionMessage actionMessage = new ActionMessage("errors.bag.empty");
            recordError(actionMessage, request);
            return mapping.findForward("results");
        }

        InterMineBag bag = profile.getSavedBags().get(bagName);

        if ((bag != null) && (!bag.getType().equals(pt.getSelectedClass()))) {
            ActionMessage actionMessage = new ActionMessage("bag.moreThanOneType");
            recordError(actionMessage, request);
            return mapping.findForward("results");
        }

        try {
            if (bag == null) {
                InterMineAPI im = SessionMethods.getInterMineAPI(session);
                bag = profile.createBag(bagName, pt.getSelectedClass(), "", im.getClassKeys());
            }
            pt.addSelectedToBag(bag);
            recordMessage(new ActionMessage("bag.saved", bagName), request);
            SessionMethods.invalidateBagTable(session, bagName);
            //tracks the list creation
            if ("saveNewBag".equals(operation)) {
                InterMineAPI im = SessionMethods.getInterMineAPI(session);
                im.getTrackerDelegate().trackListCreation(bag.getType(), bag.getSize(),
                                        ListBuildMode.QUERY, profile, session.getId());
            }
        } catch (Exception e) {
            LOG.error("Failed to save bag", e);
            recordError(new ActionMessage("An error occured while saving the bag"), request);
            return mapping.findForward("results");
        }

        if ("saveNewBag".equals(operation)) {
            ForwardParameters forwardParameters = new ForwardParameters(mapping.findForward("bag"));
            forwardParameters.addParameter("bagName", bag.getName());
            forwardParameters.addParameter("trackExecution", "false");
            return forwardParameters.forward();
        }
        return mapping.findForward("results");
    }
}
