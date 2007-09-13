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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.model.userprofile.Tag;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.tagging.TagTypes;
import org.intermine.web.logic.template.TemplateQuery;

/**
 * Controller for the favourites tile responsible for getting and displaying the
 * list of favourite templates
 * 
 * @author Xavier Watkins
 * 
 */
public class FavouritesController extends TilesAction
{

    /**
     * {@inheritDoc}
     */
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context, 
                                 @SuppressWarnings("unused") ActionMapping mapping, 
                                 @SuppressWarnings("unused") ActionForm form,
            HttpServletRequest request, 
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
        ArrayList favouriteTemplates = new ArrayList();
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ProfileManager pm = (ProfileManager) request.getSession().getServletContext().getAttribute(
                Constants.PROFILE_MANAGER);
        // Only continue if the user is logged in
        if (profile.getUsername() != null) {
            String sup = (String) servletContext.getAttribute(Constants.SUPERUSER_ACCOUNT);
            Profile superuserProfile = pm.getProfile(sup);

            Map savedTemplates = new HashMap();
            savedTemplates.putAll(superuserProfile.getSavedTemplates());
            savedTemplates.putAll(profile.getSavedTemplates());

            List userTags = pm.getTags("favourite", null, TagTypes.TEMPLATE, profile.getUsername());
            for (Iterator iter = userTags.iterator(); iter.hasNext();) {
                Tag element = (Tag) iter.next();
                TemplateQuery templateQuery =
                    (TemplateQuery) savedTemplates.get(element.getObjectIdentifier());
                if (templateQuery != null) {
                    favouriteTemplates.add(templateQuery);
                }
            }
        }
        servletContext.setAttribute("favouriteTemplates", favouriteTemplates);
        return null;
    }
}
