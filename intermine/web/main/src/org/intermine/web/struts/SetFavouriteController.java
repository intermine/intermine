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

import java.util.Iterator;
import java.util.List;

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
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        String name = (String) context.getAttribute("name");
        String type = (String) context.getAttribute("type");
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ProfileManager pm = (ProfileManager) request.getSession().getServletContext().getAttribute(
                Constants.PROFILE_MANAGER);
        List userTags = null;
        if (type.equals(TagTypes.TEMPLATE)) {
            userTags = pm.getTags(null, name, TagTypes.TEMPLATE, profile.getUsername());
        } else if (type.equals(TagTypes.BAG)) {
            userTags = pm.getTags(null, name, TagTypes.BAG, profile.getUsername());
        } else {
            throw new RuntimeException("Wrong tag type:" + type);
        }
        String isFavourite = "false";
        for (Iterator iter = userTags.iterator(); iter.hasNext();) {
            Tag tag = (Tag) iter.next();
            if (tag.getTagName().equals("favourite")) {
                isFavourite = "true";
                break;
            }
        }
        request.setAttribute("isFavourite", isFavourite);
        return null;
    }

}
