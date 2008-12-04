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

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
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
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.query.ObjectStoreBagCombination;
import org.intermine.objectstore.query.Query;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.BagHelper;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
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

        // TODO why isn't validateBagName() catching this?
        if (selectedBagNames.length == 0) {
            recordError(new ActionMessage("errors.bag.listnotselected"), request);
            return getReturn(mbf.getPageName(), mapping);
        }

        if (request.getParameter("union") != null
                || (mbf.getListsButton() != null && mbf.getListsButton()
                        .equals("union"))) {
            combine(mapping, form, request, ObjectStoreBagCombination.UNION,
                    "UNION");
        } else if (request.getParameter("intersect") != null
                || (mbf.getListsButton() != null && mbf.getListsButton()
                        .equals("intersect"))) {
            combine(mapping, form, request,
                    ObjectStoreBagCombination.INTERSECT, "INTERSECT");
        } else if (request.getParameter("subtract") != null
                || (mbf.getListsButton() != null && mbf.getListsButton()
                        .equals("subtract"))) {
            combine(mapping, form, request,
                    ObjectStoreBagCombination.ALLBUTINTERSECT, "SUBTRACT");
        } else if (request.getParameter("delete") != null
                || (mbf.getListsButton() != null && mbf.getListsButton()
                        .equals("delete"))) {
            delete(mapping, form, request);
        } else if (request.getParameter("copy") != null
                || (mbf.getListsButton() != null && mbf.getListsButton()
                        .equals("copy"))) {
            copy(mapping, form, request);
        }

        return getReturn(mbf.getPageName(), mapping);
    }

    private void copy(@SuppressWarnings("unused") ActionMapping mapping, ActionForm form,
        HttpServletRequest request) throws ObjectStoreException {

        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ModifyBagForm frm = (ModifyBagForm) form;
        String[] selectedBagNames = frm.getSelectedBags();


        Map<String, InterMineBag> allBags = WebUtil.getAllBags
            (profile.getSavedBags(), request.getSession().getServletContext());
        ObjectStoreWriter userOSW = profile.getProfileManager().getProfileObjectStoreWriter();

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
            if (createBag(origBag, newBagName, allBags, userOSW, profile, request)) {
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
                if (createBag(origBag, newBagName, allBags, userOSW, profile, request)) {
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

    private boolean createBag(InterMineBag origBag, String newBagName,
                              @SuppressWarnings("unused") Map<String, InterMineBag> allBags,
                              ObjectStoreWriter userOSW,
                              Profile profile,
                              @SuppressWarnings("unused") HttpServletRequest request)
            throws ObjectStoreException {
        // Clone method clones the bag in the database
        InterMineBag newBag = (InterMineBag) origBag.clone(userOSW);
        newBag.setDate(new Date());
        newBag.setName(newBagName, userOSW);
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

    /**
     * Union the selected bags
     *
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param op
     *            the operation to pass to the ObjectStoreBagCombination
     *            constructor
     * @param opText
     *            the operation's name
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws an exception
     */
    public ActionForward combine(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, int op, String opText) throws Exception {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ModifyBagForm mbf = (ModifyBagForm) form;
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext
                .getAttribute(Constants.OBJECTSTORE);

        Map<String, InterMineBag> allBags = WebUtil.getAllBags(profile
                .getSavedBags(), servletContext);

        String[] selectedBags = mbf.getSelectedBags();

        String type = getTypesMatch(allBags, selectedBags, os);
        if (type == null) {
            // TODO this didn't get message from message bundle correctly
            SessionMethods.recordError(
                    "You can only perform operations on lists of the same type."
                            + " Lists "
                            + StringUtil
                                    .prettyList(Arrays.asList(selectedBags))
                            + " do not match.", session);
            return getReturn(mbf.getPageName(), mapping);
        }

        // Now combine
        String name = BagHelper.findNewBagName(allBags, mbf.getNewBagName());
        ObjectStoreWriter uosw = profile.getProfileManager()
                .getProfileObjectStoreWriter();
        InterMineBag combined = new InterMineBag(name, type, null, new Date(),
                os, profile.getUserId(), uosw);
        ObjectStoreBagCombination osbc = new ObjectStoreBagCombination(op);
        for (int i = 0; i < selectedBags.length; i++) {
            osbc.addBag(allBags.get(selectedBags[i]).getOsb());
        }
        Query q = new Query();
        q.addToSelect(osbc);
        ObjectStoreWriter osw = null;
        try {
            osw = new ObjectStoreWriterInterMineImpl(os);
            osw.addToBagFromQuery(combined.getOsb(), q);
        } catch (ObjectStoreException e) {
            LOG.error(e);
            ActionMessage actionMessage = new ActionMessage(
                    "An error occurred while saving the list");
            recordError(actionMessage, request);
            return getReturn(mbf.getPageName(), mapping);
        } finally {
            try {
                if (osw != null) {
                    osw.close();
                }
            } catch (ObjectStoreException e) {
                // empty
            }
        }

        if (combined.size() > 0) {
            profile.saveBag(name, combined);
            SessionMethods.recordMessage("Created list \"" + combined.getName()
                    + "\" as " + opText + " of  "
                    + StringUtil.prettyList(Arrays.asList(selectedBags)) + ".",
                    session);
        } else {
            deleteBag(session, profile, combined);
            SessionMethods.recordError(opText + " operation on lists "
                    + StringUtil.prettyList(Arrays.asList(selectedBags))
                    + " produced no results.", session);
        }

        return getReturn(mbf.getPageName(), mapping);
    }

    /**
     * Given a set of bag names, find out whether they are all of the same type.
     *
     * @param bags
     *            map from bag name to InterMineIdBag subclass
     * @param selectedBags
     *            names of bags to match
     * @param os
     *            the obejct store
     * @return a String containing the type name or null if there was no match
     */
    private static String getTypesMatch(Map bags, String selectedBags[],
            ObjectStore os) {
        // Check that all selected bags are of the same type
        String type = ((InterMineBag) bags.get(selectedBags[0])).getType();
        Model model = os.getModel();
        String packageName = model.getPackageName();
        StringBuffer className = new StringBuffer(packageName + "." + type);
        try {
            Set classDescriptors = model.getClassDescriptorsForClass(Class
                    .forName(className.toString()));
            for (int i = 1; i < selectedBags.length; i++) {
                boolean currentMatch = false;
                String otherType = ((InterMineBag) bags.get(selectedBags[i]))
                        .getType();
                for (Iterator iter = classDescriptors.iterator(); iter
                        .hasNext();) {
                    ClassDescriptor cld = (ClassDescriptor) iter.next();
                    if (otherType.equals(TypeUtil
                            .unqualifiedName(cld.getName()))) {
                        currentMatch = true;
                    }
                }
                if (!currentMatch) {
                    return null;
                }
            }
        } catch (ClassNotFoundException e) {
            LOG.error("error while matching bag types", e);
        }
        return type;
    }

    /**
     * Delete the selected bags
     *
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws an exception
     */
    public ActionForward delete(ActionMapping mapping, ActionForm form,
            HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);

        ModifyBagForm mbf = (ModifyBagForm) form;
        for (int i = 0; i < mbf.getSelectedBags().length; i++) {
            InterMineBag bag = profile.getSavedBags().get(
                    mbf.getSelectedBags()[i]);
            deleteBag(session, profile, bag);
        }

        return getReturn(mbf.getPageName(), mapping);
    }

    // Remove a bag from userprofile database and session cache
    private void deleteBag(HttpSession session, Profile profile,
            InterMineBag bag) throws ObjectStoreException {
        ObjectStoreWriter uosw = profile.getProfileManager()
                .getProfileObjectStoreWriter();
        // removed a cached bag table from the session
        SessionMethods.invalidateBagTable(session, bag.getName());
        bag.setProfileId(null, uosw); // Deletes from database
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
