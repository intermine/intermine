package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2009 FlyMine
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.TagManager;
import org.intermine.api.tag.TagNames;
import org.intermine.api.tag.TagTypes;
import org.intermine.api.template.TemplateQuery;
import org.intermine.model.userprofile.Tag;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.session.SessionMethods;

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
                                 @SuppressWarnings("unused") HttpServletResponse response) 
    throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);

        ArrayList<TemplateQuery> favouriteTemplates = new ArrayList();
        
        if (profile.getUsername() != null) {
            Profile superuserProfile = im.getProfileManager().getSuperuserProfile();
            Map<String, TemplateQuery> savedTemplates = new HashMap();
            savedTemplates.putAll(superuserProfile.getSavedTemplates());
            savedTemplates.putAll(profile.getSavedTemplates());
            TagManager tagManager = im.getTagManager();

            List userTags = tagManager.getTags(TagNames.IM_FAVOURITE, null, TagTypes.TEMPLATE, 
                                               profile.getUsername());
            for (Iterator iter = userTags.iterator(); iter.hasNext();) {
                Tag element = (Tag) iter.next();
                TemplateQuery templateQuery = savedTemplates.get(element.getObjectIdentifier());
                if (templateQuery != null) {
                    favouriteTemplates.add(templateQuery);
                }
            }
        }
        session.getServletContext().setAttribute("favouriteTemplates", favouriteTemplates);
        return null;
    }
}
