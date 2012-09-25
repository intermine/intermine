package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.TagManager;
import org.intermine.api.tag.TagNames;
import org.intermine.api.tag.TagTypes;
import org.intermine.model.userprofile.Tag;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Controller for the inline tag editing tile
 * @author dbutano
 */
public class SharingBagUsersController extends TilesAction
{

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        String bagName = (String) context.getAttribute("bagName");
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        Profile profile = SessionMethods.getProfile(request.getSession());
        BagManager bm = im.getBagManager();
        request.setAttribute("currentSharingUsers", bm.getUsersSharingBag(bagName,
            profile.getUsername()));
        TagManager tm = im.getTagManager();
        List<Tag> tags = tm.getTags(TagNames.IM_PUBLIC, bagName, TagTypes.BAG,
                                    profile.getUsername());
        if (tags.isEmpty() || !profile.isSuperuser()) {
            request.setAttribute("shareBags", true);
        } else {
            request.setAttribute("shareBags", false);
        }
        return null;
    }

}
