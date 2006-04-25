package org.intermine.dwr;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.intermine.web.Constants;
import org.intermine.web.Profile;
import org.intermine.web.ProfileManager;
import org.intermine.web.tagging.TagTypes;

import uk.ltd.getahead.dwr.WebContext;
import uk.ltd.getahead.dwr.WebContextFactory;

/**
 * This class contains the methods called through DWR Ajax
 * 
 * @author Xavier Watkins
 * 
 */
public class AjaxServices
{

    /**
     * Creates a favourite Tag for the given templateName
     * 
     * @param templateName
     *            the name of the template we want to set as a favourite
     */
    public void setFavouriteTemplate(String templateName) {
        WebContext ctx = WebContextFactory.get();
        HttpSession session = ctx.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        HttpServletRequest request = ctx.getHttpServletRequest();
        templateName = templateName.replaceAll("#039;", "'");
        ProfileManager pm = (ProfileManager) request.getSession().getServletContext().getAttribute(
                Constants.PROFILE_MANAGER);
        pm.addTag("favourite", templateName, TagTypes.TEMPLATE, profile.getUsername());
    }
}
