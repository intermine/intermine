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

import java.util.Set;

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
import org.intermine.web.logic.profile.TagManager;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.tagging.TagNames;

/**
 * Controller for the starTemplate tile. This tile handles the display of the
 * star which lets you set a template as a favourite, using DWR Ajax.
 *
 * @author Xavier Watkins
 *
 */
public class SetFavouriteController extends TilesAction
{

    /**
     * {@inheritDoc}
     */
    public ActionForward execute(ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
    throws Exception {
        String name = (String) context.getAttribute("name");
        String type = (String) context.getAttribute("type");
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        TagManager tagManager = SessionMethods.getTagManager(session);
        Set<String> userTags = tagManager.getObjectTagNames(name, type, profile.getUsername());
        String isFavourite = "false";
        for (String tag : userTags) {
            if (tag.equals(TagNames.IM_FAVOURITE)) {
                isFavourite = "true";
                break;
            }
        }
        request.setAttribute("isFavourite", isFavourite);
        return null;
    }

}
