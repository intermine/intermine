package org.intermine.web.tagging;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.model.userprofile.Tag;
import org.intermine.web.Constants;
import org.intermine.web.Profile;
import org.intermine.web.ProfileManager;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;

/**
 * Controller for the inline tag editing tile - all actions here are called via
 * javascript.
 * 
 * @author Thomas Riley
 */
public class InlineTagEditorChange extends DispatchAction
{
    private static final Logger LOG = Logger.getLogger(InlineTagEditorChange.class);
    
    /**
     * Add a tag.
     * @see DispatchAction#execute
     */
    public ActionForward add(ActionMapping mapping,
            ActionForm form,
            HttpServletRequest request,
            HttpServletResponse response)
            throws Exception {
        ProfileManager pm = (ProfileManager) request.getSession()
            .getServletContext().getAttribute(Constants.PROFILE_MANAGER);
        Profile profile = (Profile) request.getSession().getAttribute(Constants.PROFILE);
        
        String uid = request.getParameter("uid");
        String type = request.getParameter("type");
        String tag = request.getParameter("tag");
        
        LOG.info("adding tag " + tag + " uid " + uid + " type " + type);
        
        if (profile.getUsername() != null && !StringUtils.isEmpty(tag)
            && !StringUtils.isEmpty(type)
            && !StringUtils.isEmpty(uid)) {
            pm.addTag(tag, uid, type, profile.getUsername());
        }
        return null;
    }
    
    /**
     * Delete a tag.
     * @see DispatchAction#execute
     */
    public ActionForward delete(ActionMapping mapping,
            ActionForm form,
            HttpServletRequest request,
            HttpServletResponse response)
            throws Exception {
        ProfileManager pm = (ProfileManager) request.getSession()
            .getServletContext().getAttribute(Constants.PROFILE_MANAGER);
        Profile profile = (Profile) request.getSession().getAttribute(Constants.PROFILE);
        
        String tagid = request.getParameter("tagid");
        if (!StringUtils.isEmpty(tagid)) {
            Tag tag = pm.getTagById(Integer.parseInt(tagid));
            // only let users delete their own tags
            if (tag.getUserProfile().getUsername().equals(profile.getUsername())) {
                pm.deleteTag(tag);
            }
        }
        return null;
    }
    
    /**
     * Render the current tags.
     * @see DispatchAction#execute
     */
    public ActionForward currentTags(ActionMapping mapping,
            ActionForm form,
            HttpServletRequest request,
            HttpServletResponse response)
            throws Exception {
        ProfileManager pm = (ProfileManager) request.getSession()
            .getServletContext().getAttribute(Constants.PROFILE_MANAGER);
        Profile profile = (Profile) request.getSession().getAttribute(Constants.PROFILE);
        
        String uid = request.getParameter("uid");
        String type = request.getParameter("type");
        
        if (profile.getUsername() != null
            && !StringUtils.isEmpty(type)
            && !StringUtils.isEmpty(uid)) {
            request.setAttribute("uid", uid);
            request.setAttribute("type", type);
            request.setAttribute("currentTags", pm.getTags(null, uid, type, profile.getUsername()));
        }
        return mapping.findForward("currentTags");
    }
}