package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
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
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.TagManager;
import org.intermine.api.tag.TagNames;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Controller for the starTemplate tile. This tile handles the display of the
 * star which lets you set a template as a favourite, using DWR Ajax.
 *
 * @author Xavier Watkins
 *
 */
public class FavouritesController extends TilesAction
{

    /**
     * {@inheritDoc}
     */
    public ActionForward execute(ComponentContext context,
            ActionMapping mapping,
            ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        String name = (String) context.getAttribute("name");
        String type = (String) context.getAttribute("type");
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);

        Profile profile = SessionMethods.getProfile(session);
        TagManager tagManager = im.getTagManager();

        Set<String> userTags = tagManager.getObjectTagNames(name, type, profile.getUsername());
        String isFavourite = Boolean.toString(userTags.contains(TagNames.IM_FAVOURITE));

        request.setAttribute("isFavourite", isFavourite);
        return null;
    }
}
