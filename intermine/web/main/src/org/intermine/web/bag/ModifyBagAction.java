package org.intermine.web.bag;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
import org.intermine.util.TypeUtil;
import org.intermine.web.Constants;
import org.intermine.web.InterMineAction;
import org.intermine.web.Profile;
import org.intermine.web.SessionMethods;
import org.intermine.web.WebUtil;

/**
 * Implementation of <strong>Action</strong> to modify bags
 * @author Mark Woodbridge
 */
public class ModifyBagAction extends InterMineAction
{
    private static final Logger LOG = Logger.getLogger(InterMineAction.class);
    
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
                                 HttpServletResponse response)
        throws Exception {
        if (request.getParameter("union") != null) {
            union(mapping, form, request, response);
        } else if (request.getParameter("intersect") != null) {
            intersect(mapping, form, request, response);
        } else if (request.getParameter("subtract") != null) {
            subtract(mapping, form, request, response);
        } else if (request.getParameter("delete") != null) {
            delete(mapping, form, request, response);
        }
        return mapping.findForward("bag");
    }

    /**
     * Union the selected bags
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward union(ActionMapping mapping,
                               ActionForm form,
                               HttpServletRequest request,
                               HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ModifyBagForm mbf = (ModifyBagForm) form;
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);

        Map savedBags = profile.getSavedBags();
        String[] selectedBags = mbf.getSelectedBags();

        String type = getTypesMatch(savedBags, selectedBags, os);
        if (type == null) {
            recordError(new ActionMessage("bag.typesDontMatch"), request);
            return mapping.findForward("bag");
        }

        // Now combine
        String name = BagHelper.findNewBagName(savedBags, mbf.getNewBagName());
        ObjectStore profileOs = profile.getProfileManager().getUserProfileObjectStore();
        Collection union = new ArrayList();
        union.addAll((Collection) savedBags.get(selectedBags[0]));
        for (int i = 1; i < selectedBags.length; i++) {
            union.addAll((Collection) savedBags.get(selectedBags[i]));
        }
        InterMineBag combined =
            new InterMineBag(profile.getUserId(), name, type, profileOs, os, union);

        int defaultMax = 10000;

        int maxBagSize = WebUtil.getIntSessionProperty(session, "max.bag.size", defaultMax);

        if (combined.size () > maxBagSize) {
            ActionMessage actionMessage =
                new ActionMessage("bag.tooBig", new Integer(maxBagSize));
            recordError(actionMessage, request);

            return mapping.findForward("bag");
        }

        int maxNotLoggedSize = WebUtil.getIntSessionProperty(session, "max.bag.size.notloggedin",
                                                             Constants.MAX_NOT_LOGGED_BAG_SIZE);
        profile.saveBag(name, combined, maxNotLoggedSize);

        return mapping.findForward("bag");
    }

    /**
     * Given a set of bag names, find out whether they are all of the same type.
     * 
     * @param bags map from bag name to InterMineIdBag subclass
     * @param selectedBags names of bags to match
     * @param os the obejct store
     * @return a String containing the type name or null if there was no match
     */
    private static String getTypesMatch(Map bags, String selectedBags[], ObjectStore os) {
        // Check that all selected bags are of the same type
        String type = ((InterMineBag) bags.get(selectedBags[0])).getType();
        Model model = os.getModel();
        String packageName = model.getPackageName();
        StringBuffer className = new StringBuffer(packageName + "." + type);
        try {
            Set classDescriptors  = model.getClassDescriptorsForClass(Class.forName(
                                                                   className.toString()));
            for (int i = 1; i < selectedBags.length; i++) {
                boolean currentMatch = false;
                String otherType = ((InterMineBag) bags.get(selectedBags[i])).getType();
                for (Iterator iter = classDescriptors.iterator(); iter.hasNext();) {
                    ClassDescriptor cld = (ClassDescriptor) iter.next();
                    if (otherType.equals(TypeUtil.unqualifiedName(cld.getName()))) {
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
     * Intersect the selected bags
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward intersect(ActionMapping mapping,
                                   ActionForm form,
                                   HttpServletRequest request,
                                   HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ModifyBagForm mbf = (ModifyBagForm) form;

        Map savedBags = profile.getSavedBags();
        String[] selectedBags = mbf.getSelectedBags();

        String type = getTypesMatch(savedBags, selectedBags, os);
        if (type == null) {
            recordError(new ActionMessage("bag.typesDontMatch"), request);
            return mapping.findForward("bag");
        }

        Collection intersect = new ArrayList();
        intersect.addAll((Collection) savedBags.get(selectedBags[0]));
        for (int i = 1; i < selectedBags.length; i++) {
            intersect.retainAll((Collection) savedBags.get(selectedBags[i]));
        }

        if (intersect.size() > 0) {
            String name = BagHelper.findNewBagName(savedBags, mbf.getNewBagName());
            ObjectStore profileOs = profile.getProfileManager().getUserProfileObjectStore();
            InterMineBag combined =
                new InterMineBag(profile.getUserId(), name, type, profileOs, os, intersect);

            int maxNotLoggedSize = WebUtil.getIntSessionProperty(session,
                                                                 "max.bag.size.notloggedin",
                                                                 Constants.MAX_NOT_LOGGED_BAG_SIZE);
            profile.saveBag(name, combined, maxNotLoggedSize);
        } else {
            recordError(new ActionMessage("bag.noIntersection"), request);
        }
        return mapping.findForward("bag");
    }

    /**
     * Compute the set of objects that are in only one of the selected bags.
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward subtract(ActionMapping mapping,
                                  ActionForm form,
                                  HttpServletRequest request,
                                  HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ModifyBagForm mbf = (ModifyBagForm) form;

        Map savedBags = profile.getSavedBags();
        String[] selectedBags = mbf.getSelectedBags();
        String name = BagHelper.findNewBagName(savedBags, mbf.getNewBagName());

        String type = getTypesMatch(savedBags, selectedBags, os);
        if (type == null) {
            recordError(new ActionMessage("bag.typesDontMatch"), request);
            return mapping.findForward("bag");
        }

        // A map from objects to the number of occurrences of that object
        Map countMap = new HashMap();

        for (int i = 0; i < selectedBags.length; i++) {
            Iterator iter = ((Collection) savedBags.get(selectedBags[i])).iterator();
            while (iter.hasNext()) {
                Object thisObj = iter.next();
                if (countMap.containsKey(thisObj)) {
                    int newVal = ((Integer) countMap.get(thisObj)).intValue() + 1;
                    countMap.put(thisObj, new Integer (newVal));
                } else {
                    countMap.put(thisObj, new Integer(1));
                }
            }
        }

        Collection subtract = new ArrayList();
        Iterator iter = countMap.keySet().iterator();
        while (iter.hasNext()) {
            Object thisObj = iter.next();
            if (countMap.get(thisObj).equals(new Integer(1))) {
                subtract.add(thisObj);
            }
        }

        if (subtract.size() > 0) {
            ObjectStore profileOs = profile.getProfileManager().getUserProfileObjectStore();
            InterMineBag resultBag =
                new InterMineBag(profile.getUserId(), name, type, profileOs, os, subtract);
            int defaultMaxNotLoggedSize = 3;
            int maxNotLoggedSize = WebUtil.getIntSessionProperty(session, 
                                                                 "max.bag.size.notloggedin",
                                                                 defaultMaxNotLoggedSize);
            profile.saveBag(name, resultBag, maxNotLoggedSize);
        } else {
            recordError(new ActionMessage("bag.emptySubtraction"), request);
        }
       
        return mapping.findForward("bag");
    }

    /**
     * Delete the selected bags
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward delete(ActionMapping mapping,
                                ActionForm form,
                                HttpServletRequest request,
                                HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);

        ModifyBagForm mbf = (ModifyBagForm) form;
        for (int i = 0; i < mbf.getSelectedBags().length; i++) {
            SessionMethods.invalidateBagTable(session, mbf.getSelectedBags()[i]);
            profile.deleteBag(mbf.getSelectedBags()[i]);
        }

        return mapping.findForward("bag");
    }
}
