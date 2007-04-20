package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2007 FlyMine
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
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.query.ObjectStoreBagCombination;
import org.intermine.objectstore.query.Query;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.BagHelper;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.session.SessionMethods;

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
            combine(mapping, form, request, response, ObjectStoreBagCombination.UNION);
        } else if (request.getParameter("intersect") != null) {
            combine(mapping, form, request, response, ObjectStoreBagCombination.INTERSECT);
        } else if (request.getParameter("subtract") != null) {
            combine(mapping, form, request, response, ObjectStoreBagCombination.EXCEPT);
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
    public ActionForward combine(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response, int op) throws Exception {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ModifyBagForm mbf = (ModifyBagForm) form;
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);

        Map<String, InterMineBag> savedBags = (Map<String, InterMineBag>) profile.getSavedBags();
        String[] selectedBags = mbf.getSelectedBags();

        String type = getTypesMatch(savedBags, selectedBags, os);
        if (type == null) {
            recordError(new ActionMessage("bag.typesDontMatch"), request);
            return mapping.findForward("bag");
        }

        // Now combine
        String name = BagHelper.findNewBagName(savedBags, mbf.getNewBagName());
        ObjectStoreWriter uosw = profile.getProfileManager().getUserProfileObjectStore();
        InterMineBag combined = new InterMineBag(name, type, null, os, profile.getUserId(), uosw);
        ObjectStoreBagCombination osbc = new ObjectStoreBagCombination(op);
        for (int i = 0; i < selectedBags.length; i++) {
            osbc.addBag(savedBags.get(selectedBags[i]).getOsb());
        }
        Query q = new Query();
        q.addToSelect(osbc);
        ObjectStoreWriter osw = null;
        try {
            osw = new ObjectStoreWriterInterMineImpl(os);
            LOG.error("Combining bags with query: " + q);
            osw.addToBagFromQuery(combined.getOsb(), q);
        } catch (ObjectStoreException e) {
            LOG.error(e);
            ActionMessage actionMessage = new ActionMessage(
                    "An error occurred while saving the bag");
            recordError(actionMessage, request);
            return mapping.findForward("bag");
        } finally {
            try {
                if (osw != null) {
                    osw.close();
                }
            } catch (ObjectStoreException e) {
            }
        }

/*        int defaultMax = 10000;
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
*/
        profile.saveBag(name, combined);
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
