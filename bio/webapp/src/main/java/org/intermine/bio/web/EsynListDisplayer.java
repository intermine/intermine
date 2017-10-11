package org.intermine.bio.web;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.bio.util.BioUtil;
import org.intermine.metadata.StringUtil;
import org.intermine.web.logic.bag.BagHelper;
import org.intermine.web.logic.session.SessionMethods;

/**
 * displayer for esyn on list analysis page
 * @author Julie Sullivan
 */
public class EsynListDisplayer extends TilesAction
{
    private static final String IDENTIFIER = "primaryIdentifier";
    private static final String DELIMITER = "|";

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) {

        InterMineBag bag = (InterMineBag) request.getAttribute("bag");
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());

        String identifiers = BagHelper.getAttributesFromBag(bag, im.getObjectStore(), "",
                IDENTIFIER, DELIMITER);
        request.setAttribute("identifiers", identifiers);

        Collection<String> organismsInBag = BioUtil.getOrganisms(im.getObjectStore(), bag.getType(),
                bag.getContentsAsIds(), false, "taxonId");
        String organisms = null;
        if (!organismsInBag.isEmpty()) {
            organisms = StringUtil.join(organismsInBag, ",");
        }
        if (StringUtils.isNotEmpty(organisms)) {
            request.setAttribute("taxon", organisms);
        }
        return null;
    }
}
