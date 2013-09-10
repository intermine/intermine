package org.modmine.web;

/*
 * Copyright (C) 2002-2013 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.web.logic.session.SessionMethods;
/**
 *
 * @author sc486
 *
 */

public class FeaturesOverlapsController extends TilesAction
{
    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context,
            ActionMapping mapping,
            ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());

        InterMineBag bag = (InterMineBag) request.getAttribute("bag");
        FeaturesOverlapsAction.initFeaturesOverlaps(im, bag);
        String givenFeatureType = bag.getType();

        request.setAttribute("givenFeatureType", givenFeatureType);
        request.setAttribute("featuresList", bag);


        return null;
    }
}