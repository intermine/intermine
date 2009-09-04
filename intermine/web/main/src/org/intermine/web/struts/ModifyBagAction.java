package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.bag.BagManager;
import org.intermine.api.bag.BagOperations;
import org.intermine.api.bag.IncompatibleBagTypesException;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.BagHelper;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Implementation of <strong>Action</strong> to modify bags
 *
 * @author Mark Woodbridge
 */
public class ModifyBagAction extends InterMineAction
{
    private static final Logger LOG = Logger.getLogger(InterMineAction.class);

    /**
     * Forward to the correct method based on the button pressed
     *
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws an exception
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, @SuppressWarnings("unused")
            HttpServletResponse response) throws Exception {
        ModifyBagForm mbf = (ModifyBagForm) form;
        String[] selectedBagNames = mbf.getSelectedBags();

        // This should already be caught by Ajax code
        if (selectedBagNames.length == 0) {
            recordError(new ActionMessage("errors.bag.listnotselected"), request);
            return getReturn(mbf.getPageName(), mapping);
        }

        if (request.getParameter("union") != null
                || (mbf.getListsButton() != null && mbf.getListsButton()
                        .equals("union"))) {
            combine(form, request, BagOperations.UNION);
        } else if (request.getParameter("intersect") != null
                || (mbf.getListsButton() != null && mbf.getListsButton()
                        .equals("intersect"))) {
            combine(form, request, BagOperations.INTERSECT);
        } else if (request.getParameter("subtract") != null
                || (mbf.getListsButton() != null && mbf.getListsButton()
                        .equals("subtract"))) {
            combine(form, request, BagOperations.SUBTRACT);
        } else if (request.getParameter("delete") != null
                || (mbf.getListsButton() != null && mbf.getListsButton()
                        .equals("delete"))) {
            delete(form, request);
        } else if (request.getParameter("copy") != null
                || (mbf.getListsButton() != null && mbf.getListsButton()
                        .equals("copy"))) {
            copy(form, request);
        }

        return getReturn(mbf.getPageName(), mapping);
    }

    private void copy(ActionForm form, HttpServletRequest request) throws ObjectStoreException {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ModifyBagForm frm = (ModifyBagForm) form;
        String[] selectedBagNames = frm.getSelectedBags();
        
        BagManager bagManager = SessionMethods.getBagManager(session.getServletContext());
        Map<String, InterMineBag> allBags = bagManager.getUserAndGlobalBags(profile);

        String newNameTextBox = getNewNameTextBox(request, frm);

        if (selectedBagNames.length == 1) {
            String selectedBagName = selectedBagNames[0];
            InterMineBag origBag = allBags.get(selectedBagName);

            String newBagName;
            if (newNameTextBox != null) {
                newBagName = newNameTextBox;
            } else {
                newBagName = generateNewName(selectedBagName, allBags);
            }
            if (origBag == null) {
                recordError(new ActionMessage("errors.bag.notfound"), request);
                return;
            }
            if (createBag(origBag, newBagName, profile)) {
                recordMessage(new ActionMessage("bag.createdlists", newBagName), request);
            }
        } else {
            if (newNameTextBox != null) {
                recordError(new ActionMessage("errors.bag.namecannotbespecified"), request);
                return;
            }
            String msg = "";
            for (int i = 0; i < selectedBagNames.length; i++) {
                String selectedBagName = selectedBagNames[i];
                String newBagName = generateNewName(selectedBagName, allBags);
                InterMineBag origBag = allBags.get(selectedBagName);
                if (origBag == null) {
                    recordError(new ActionMessage("errors.bag.notfound"), request);
                    return;
                }
                if (createBag(origBag, newBagName, profile)) {
                    msg += newBagName + ", ";
                }
            }
            if (msg.length() > 2) {
                msg = msg.substring(0, msg.length() - 2);
            }
            if (msg.length() > 0) {
                recordMessage(new ActionMessage("bag.createdlists", msg), request);
            }
        }
    }

    private String getNewNameTextBox(HttpServletRequest request,
            ModifyBagForm frm) {
        String ret = null;
        Properties properties = (Properties) request.getSession()
                .getServletContext().getAttribute(Constants.WEB_PROPERTIES);
        String defaultName = properties.getProperty("lists.input.example");
        if (frm.getNewBagName() != null
                && frm.getNewBagName().trim().length() > 0
                && !frm.getNewBagName().equalsIgnoreCase(defaultName)) {
            ret = frm.getNewBagName().trim();
        }
        return ret;
    }

    private boolean createBag(InterMineBag origBag, String newBagName, Profile profile)
    throws ObjectStoreException {
        // Clone method clones the bag in the database
        InterMineBag newBag = (InterMineBag) origBag.clone();
        newBag.setDate(new Date());
        newBag.setName(newBagName);
        profile.saveBag(newBagName, newBag);
        return true;
    }

    private String generateNewName(String origName, Map<String, InterMineBag> allBags) {
        int i = 1;
        while (allBags.get(origName + "_copy" + i) != null) {
            i++;
        }
        return origName + "_copy" + i;
    }


    private void combine(ActionForm form, HttpServletRequest request, String opText) {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ModifyBagForm mbf = (ModifyBagForm) form;
        ServletContext servletContext = session.getServletContext();
        
        BagManager bagManager = SessionMethods.getBagManager(servletContext);        
        Map<String, InterMineBag> allBags = bagManager.getUserAndGlobalBags(profile);

        String[] selectedBagNames = mbf.getSelectedBags();

        Collection<InterMineBag> selectedBags = getSelectedBags(allBags, selectedBagNames);
        
        String newBagName = BagHelper.findNewBagName(allBags.keySet(), mbf.getNewBagName());

        int newBagSize = 0;
        try {
            if (opText.equals(BagOperations.UNION)) {
                newBagSize = BagOperations.union(selectedBags, newBagName, profile);
            } else if (opText.equals(BagOperations.INTERSECT)) {
                newBagSize = BagOperations.intersect(selectedBags, newBagName, profile);
            } else if (opText.equals(BagOperations.SUBTRACT)) {
                newBagSize = BagOperations.subtract(selectedBags, newBagName, profile);
            }
        } catch (IncompatibleBagTypesException e) {
            SessionMethods.recordError(
                    "You can only perform operations on lists of the same type."
                            + " Lists " + StringUtil.prettyList(Arrays.asList(selectedBagNames))
                            + " do not match.", session);
            return;
        } catch (ObjectStoreException e) {
            LOG.error(e);
            ActionMessage actionMessage = new ActionMessage(
                    "An error occurred while saving the list");
            recordError(actionMessage, request);
            return;
        }

        if (newBagSize > 0) {
            SessionMethods.recordMessage("Created list \"" + newBagName
                    + "\" as " + opText + " of  "
                    + StringUtil.prettyList(Arrays.asList(selectedBagNames)) + ".",
                    session);
        } else {
            SessionMethods.recordError(opText + " operation on lists "
                    + StringUtil.prettyList(Arrays.asList(selectedBagNames))
                    + " produced no results.", session);
        }
    }

    private Collection<InterMineBag> getSelectedBags(Map<String, InterMineBag> allBags,
            String[] selectedBagNames) {
        Set<InterMineBag> selectedBags = new HashSet<InterMineBag>();
        for (String bagName : selectedBagNames) {
            selectedBags.add(allBags.get(bagName));
        }
        return selectedBags;
    }


    private void delete(ActionForm form, HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);

        ModifyBagForm mbf = (ModifyBagForm) form;
        for (int i = 0; i < mbf.getSelectedBags().length; i++) {
            InterMineBag bag = profile.getSavedBags().get(
                    mbf.getSelectedBags()[i]);
            deleteBag(session, profile, bag);
        }
    }

    // Remove a bag from userprofile database and session cache
    private void deleteBag(HttpSession session, Profile profile,
            InterMineBag bag) throws ObjectStoreException {
        // removed a cached bag table from the session
        SessionMethods.invalidateBagTable(session, bag.getName());
        bag.setProfileId(null); // Deletes from database
        profile.deleteBag(bag.getName());
    }

    private ActionForward getReturn(String pageName, ActionMapping mapping) {
        if (pageName != null && pageName.equals("MyMine")) {
            return new ForwardParameters(mapping.findForward("mymine"))
                    .addParameter("subtab", "lists").forward();
        }
        return new ForwardParameters(mapping.findForward("bag"))
                    .addParameter("subtab", "view").forward();
    }
}
