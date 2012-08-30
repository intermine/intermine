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
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.bag.BagOperations;
import org.intermine.api.bag.IncompatibleTypesException;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.SavedQuery;
import org.intermine.api.tracker.util.ListBuildMode;
import org.intermine.api.util.NameUtil;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.StringUtil;
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
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws an exception
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        ModifyBagForm mbf = (ModifyBagForm) form;
        String[] selectedBagNames = mbf.getSelectedBags();

        // This should already be caught by Ajax code
        if (selectedBagNames.length == 0) {
            recordError(new ActionMessage("errors.bag.listnotselected"), request);
            return getReturn(mbf.getPageName(), mapping);
        }

        if (request.getParameter("union") != null
                || (mbf.getListsButton() != null && "union".equals(mbf.getListsButton()))) {
            combine(form, request, BagOperations.UNION);
        } else if (request.getParameter("intersect") != null
                || (mbf.getListsButton() != null && "intersect".equals(mbf.getListsButton()))) {
            combine(form, request, BagOperations.INTERSECT);
        } else if (request.getParameter("subtract") != null
                || (mbf.getListsButton() != null && "subtract".equals(mbf.getListsButton()))) {
            combine(form, request, BagOperations.SUBTRACT);
        } else if (request.getParameter("delete") != null
                || (mbf.getListsButton() != null && "delete".equals(mbf.getListsButton()))) {
            delete(form, request);
        } else if (request.getParameter("copy") != null
                || (mbf.getListsButton() != null && "copy".equals(mbf.getListsButton()))) {
            copy(form, request);
        }

        return getReturn(mbf.getPageName(), mapping);
    }

    // make sure new list name doesn't equal the default example list name
    private String getNewNameTextBox(HttpServletRequest request, String newBagName) {
        Properties properties = SessionMethods.getWebProperties(request.getSession()
                .getServletContext());
        String exampleName = properties.getProperty("lists.input.example");
        if (StringUtils.isEmpty(newBagName) || newBagName.equalsIgnoreCase(exampleName)) {
            return null;
        }
        return newBagName;
    }

    private void copy(ActionForm form, HttpServletRequest request) throws ObjectStoreException {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Profile profile = SessionMethods.getProfile(session);
        ModifyBagForm frm = (ModifyBagForm) form;
        String[] selectedBagNames = frm.getSelectedBags();

        BagManager bagManager = im.getBagManager();
        Map<String, InterMineBag> allBags = bagManager.getBags(profile);

        String newNameTextBox = getNewNameTextBox(request, frm.getNewBagName());

        if (selectedBagNames.length == 1) {
            String selectedBagName = selectedBagNames[0];
            InterMineBag origBag = allBags.get(selectedBagName);

            if (origBag == null) {
                recordError(new ActionMessage("errors.bag.notfound"), request);
                return;
            }

            String newBagName = "";
            if (newNameTextBox != null) {
                newBagName = NameUtil.validateName(allBags.keySet(), newNameTextBox);
                if (newBagName.isEmpty()) {
                    recordError(new ActionMessage("bag.createdlists.notvalidname",
                                  newNameTextBox), request);
                    return;
                }
            }
            if (newNameTextBox == null) {
                newBagName = NameUtil.generateNewName(allBags.keySet(), selectedBagName);
            }

            if (createBag(origBag, newBagName, profile)) {
                recordMessage(new ActionMessage("bag.createdlists", newBagName), request);
                //track the list creation
                im.getTrackerDelegate().trackListCreation(origBag.getType(), origBag.getSize(),
                                        ListBuildMode.OPERATION, profile, session.getId());
            }
        } else {
            if (newNameTextBox != null) {
                recordError(new ActionMessage("errors.bag.namecannotbespecified"), request);
                return;
            }
            String msg = "";
            for (int i = 0; i < selectedBagNames.length; i++) {

                String selectedBagName = selectedBagNames[i];
                InterMineBag origBag = allBags.get(selectedBagName);

                if (origBag == null) {
                    recordError(new ActionMessage("errors.bag.notfound"), request);
                    return;
                }

                String newBagName = NameUtil.generateNewName(allBags.keySet(), selectedBagName);
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

    private boolean createBag(InterMineBag origBag, String newBagName, Profile profile)
        throws ObjectStoreException {
        // Clone method clones the bag in the database
        InterMineBag newBag = (InterMineBag) origBag.clone();
        newBag.setDate(new Date());
        newBag.setName(newBagName);
        profile.saveBag(newBagName, newBag);
        newBag.addBagValues();
        return true;
    }

    private void combine(ActionForm form, HttpServletRequest request, String opText) {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Profile profile = SessionMethods.getProfile(session);
        ModifyBagForm mbf = (ModifyBagForm) form;

        BagManager bagManager = im.getBagManager();
        Map<String, InterMineBag> allBags = bagManager.getBags(profile);

        String[] selectedBagNames = mbf.getSelectedBags();

        Collection<InterMineBag> selectedBags = getSelectedBags(allBags, selectedBagNames);

        String newBagName = NameUtil.validateName(allBags.keySet(), mbf.getNewBagName());

        int newBagSize = 0;
        try {
            if (opText.equals(BagOperations.UNION)) {
                newBagSize = BagOperations.union(selectedBags, newBagName, profile,
                                           im.getClassKeys());
            } else if (opText.equals(BagOperations.INTERSECT)) {
                newBagSize = BagOperations.intersect(selectedBags, newBagName, profile,
                                          im.getClassKeys());
            } else if (opText.equals(BagOperations.SUBTRACT)) {
                newBagSize = BagOperations.subtract(selectedBags, newBagName, profile,
                                          im.getClassKeys());
            }
        } catch (IncompatibleTypesException e) {
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
            //track the list creation
            im.getTrackerDelegate().trackListCreation(BagOperations.getCommonBagType(
                                    selectedBags), newBagSize, ListBuildMode.OPERATION,
                                    profile, session.getId());
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
        Profile profile = SessionMethods.getProfile(session);
        ModifyBagForm mbf = (ModifyBagForm) form;
        for (int i = 0; i < mbf.getSelectedBags().length; i++) {
            InterMineBag bag = profile.getSavedBags().get(mbf.getSelectedBags()[i]);
            deleteQueriesThatMentionBag(profile, bag.getName());
            deleteBag(session, profile, bag);
        }
    }

    // Remove a bag from userprofile database and session cache
    private void deleteBag(HttpSession session, Profile profile,
            InterMineBag bag) throws ObjectStoreException {
        // removed a cached bag table from the session
        SessionMethods.invalidateBagTable(session, bag.getName());
        profile.deleteBag(bag.getName());
    }

    private ActionForward getReturn(String pageName, ActionMapping mapping) {
        if (pageName != null && "MyMine".equals(pageName)) {
            return new ForwardParameters(mapping.findForward("mymine"))
                    .addParameter("subtab", "lists").forward();
        }
        return new ForwardParameters(mapping.findForward("bag"))
                    .addParameter("subtab", "view").forward();
    }

    private static void deleteQueriesThatMentionBag(Profile profile, String bagName) {
        // delete query from history
        Map<String, SavedQuery> savedQueries = profile.getHistory();
        Set<String> savedQueriesNames = new HashSet<String>(profile.getHistory().keySet());
        for (String queryName : savedQueriesNames) {
            SavedQuery query = (SavedQuery) savedQueries.get(queryName);
            if (query.getPathQuery().getBagNames().contains(bagName)) {
                profile.deleteHistory(queryName);
            }
        }

        // delete query from saved queries
        savedQueries = profile.getSavedQueries();
        savedQueriesNames = new HashSet<String>(profile.getSavedQueries().keySet());
        for (String queryName : savedQueriesNames) {
            SavedQuery query = (SavedQuery) savedQueries.get(queryName);
            if (query.getPathQuery().getBagNames().contains(bagName)) {
                profile.deleteQuery(queryName);
            }
        }
    }
}
