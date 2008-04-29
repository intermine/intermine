package org.intermine.web.logic;

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
import java.util.Set;

import org.intermine.objectstore.query.ObjectStoreBagCombination;
import org.intermine.objectstore.query.Query;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.bag.BagHelper;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.ForwardParameters;
import org.intermine.web.struts.ModifyBagForm;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

/**
 * Utility methods for the modifying bags.
 * @author Julie Sullivan
 */
public class BagUtil
{
    protected static final Logger LOG = Logger.getLogger(BagUtil.class);
    
    /**
     * Union the selected bags
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param op the operation to pass to the ObjectStoreBagCombination constructor
     * @param opText the operation's name
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public static ActionForward combine(ActionMapping mapping, ActionForm form, HttpServletRequest
                                        request,
                                 int op, String opText) throws Exception {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ModifyBagForm mbf = (ModifyBagForm) form;
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);

        Map<String, InterMineBag> allBags =
            WebUtil.getAllBags(profile.getSavedBags(), servletContext);

        String[] selectedBags = mbf.getSelectedBags();

        String type = getTypesMatch(allBags, selectedBags, os);
        if (type == null) {
            // TODO this didn't get message from message bundle correctly
            SessionMethods.recordError("You can only perform operations on lists of the same type."
                    + " Lists "
                    + StringUtil.prettyList(Arrays.asList(selectedBags))
                    + " do not match.", session);
            return getReturn(mbf.getPageName(), mapping);
        }

        // Now combine
        String name = BagHelper.findNewBagName(allBags, mbf.getNewBagName());
        ObjectStoreWriter uosw = profile.getProfileManager().getUserProfileObjectStore();
        InterMineBag combined = new InterMineBag(name, type, null, new Date(), os,
                                                 profile.getUserId(), uosw);
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
            String actionMessage = "An error occurred while saving the list";
            SessionMethods.recordError(actionMessage, session);
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
            SessionMethods.recordMessage("Created list \"" + combined.getName() + "\" as " + opText
                                         + " of  "
                                       + StringUtil.prettyList(Arrays.asList(selectedBags))
                                       + ".", session);
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
     * these methods are used by myMine and the bag view page.  this method returns the proper
     * forward parameters based on which page the user is currently on
     * @param pageName name of page
     * @param mapping page where user should be directed
     * @return forward parameters
     */
    public static ActionForward getReturn(String pageName, ActionMapping mapping) {
        if (pageName != null && pageName.equals("MyMine")) {
            return new ForwardParameters(mapping.findForward("mymine"))
            .addParameter("subtab", "lists").forward();
        } else {
            return new ForwardParameters(mapping.findForward("bag"))
            .addParameter("subtab", "view").forward();
        }
    }
    
    /**
     * Remove a bag from userprofile database and session cache
     * @param session user's session
     * @param profile user's profile
     * @param bag user's bag
     * @throws ObjectStoreException if something goes horribly wrong
     */
    public static void deleteBag(HttpSession session, Profile profile, InterMineBag bag)
    throws ObjectStoreException {
        ObjectStoreWriter uosw = profile.getProfileManager().getUserProfileObjectStore();
        // removed a cached bag table from the session
        SessionMethods.invalidateBagTable(session, bag.getName());
        bag.setProfileId(null, uosw); // Deletes from database
        profile.deleteBag(bag.getName());
    }
    
}

