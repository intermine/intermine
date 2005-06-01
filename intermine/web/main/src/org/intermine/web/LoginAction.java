package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.web.bag.InterMineBag;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Action to handle button presses on the main tile
 * @author Mark Woodbridge
 */
public class LoginAction extends InterMineAction
{
    /** 
     * Method called when user has finished updating a constraint
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
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ProfileManager pm = (ProfileManager) servletContext.getAttribute(Constants.PROFILE_MANAGER);
        String superuser = (String) servletContext.getAttribute(Constants.SUPERUSER_ACCOUNT);
        LoginForm lf = (LoginForm) form;
        
        // Merge current history into loaded profile
        Profile currentProfile = (Profile) session.getAttribute(Constants.PROFILE);
        Map mergeQueries = Collections.EMPTY_MAP;
        Map mergeBags = Collections.EMPTY_MAP;
        if (currentProfile != null && StringUtils.isEmpty(currentProfile.getUsername())) {
            mergeQueries = new HashMap(currentProfile.getSavedQueries());
            mergeBags = new HashMap(currentProfile.getSavedBags());
        }
        
        Profile profile;
        if (pm.hasProfile(lf.getUsername())) {
            profile = pm.getProfile(lf.getUsername(), lf.getPassword());
        } else {
            profile = new Profile(pm, lf.getUsername(), lf.getPassword(),
                                  new HashMap(), new HashMap(), new HashMap());
            pm.saveProfile(profile);
        }
        session.setAttribute(Constants.PROFILE, profile);
        
        if (profile.getUsername().equals(superuser)) {
            session.setAttribute(Constants.IS_SUPERUSER, Boolean.TRUE);
        }
        
        // Merge in anonymous query history
        for (Iterator iter = mergeQueries.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            PathQuery query = (PathQuery) ((PathQuery) entry.getValue()).clone();
            String name = makeUniqueQueryName((String) entry.getKey(),
                                                profile.getSavedQueries().keySet());
            profile.saveQuery(name, query);
        }
        // Merge anonymous bags
        for (Iterator iter = mergeBags.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            InterMineBag bag = (InterMineBag) entry.getValue();
            String name = makeUniqueQueryName((String) entry.getKey(),
                                                profile.getSavedQueries().keySet());
            profile.saveBag(name, bag);
        }
        
        recordMessage(new ActionMessage("login.loggedin", lf.getUsername()), request);
        return mapping.findForward("history");
    }
    
    private String makeUniqueQueryName(String name, Set names) {
        String original = name;
        int i = 1;
        while (names.contains(name)) {
            name = original + "_" + i;
        }
        return name;
    }
}
