package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2017 FlyMine
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

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.TagManager;
import org.intermine.api.profile.Taggable;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Controller for tag display tile on the lists page
 * @author Julie
 */
public class ListTagsController extends TilesAction
{

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        Object taggable = context.getAttribute("taggable");

        String tagged = null;
        String type = null;

        if (taggable instanceof Taggable) {
            tagged = ((Taggable) taggable).getName();
            type = ((Taggable) taggable).getTagType();
        }

        Set<String> currentTags;
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        TagManager tagManager = im.getTagManager();
        currentTags = tagManager.getObjectTagNames(tagged, type, null);
        request.setAttribute("currentTags", currentTags);
        return null;
    }
}
