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

import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.model.userprofile.Tag;
import org.intermine.web.tagging.TagTypes;

/**
 * Remove the favourite tag for the specified template
 * 
 * @author Xavier Watkins
 * 
 */
public class RemoveFavouriteAction extends InterMineDispatchAction
{

    /**
     * Remove the favourite tag for a template query
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
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        String templateName = request.getParameter("name");
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ProfileManager pm = (ProfileManager) request.getSession().getServletContext().getAttribute(
                Constants.PROFILE_MANAGER);
        List tagList = (List) pm.getTags(null, templateName, TagTypes.TEMPLATE, profile
                .getUsername());
        for (Iterator iter = tagList.iterator(); iter.hasNext();) {
            Tag tag = (Tag) iter.next();
            pm.deleteTag(tag);
        }
        return mapping.findForward("mymine");
    }
}
