package org.intermine.bio.web;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.mines.FriendlyMineManager;
import org.intermine.api.mines.Mine;
import org.intermine.api.profile.InterMineBag;
import org.intermine.bio.util.BioUtil;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.bag.BagHelper;
import org.intermine.web.logic.session.SessionMethods;


/**
 * Gets list of friendly intermines to show on list analysis page.
 * TODO merge with OtherMinesController
 *
 * @author Julie Sullivan
 */
public class FriendlyMineLinkController  extends TilesAction
{
    private static final String IDENTIFIER = "primaryIdentifier";
    private static final String ALTERNATIVE_IDENTIFIER = "ensemblIdentifier";

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) {
        InterMineBag bag = (InterMineBag) request.getAttribute("bag");
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        Collection<String> organismsInBag = BioUtil.getOrganisms(im.getObjectStore(), bag, false,
                "shortName");
        String organisms = null;
        if (!organismsInBag.isEmpty()) {
            organisms = StringUtil.join(organismsInBag, ",");
        }
        String identifierField = getIdentifierField(bag);
        String identifierList = BagHelper.getAttributesFromBag(bag, im.getObjectStore(), "",
                identifierField);
        request.setAttribute("identifiers", identifierList);
        if (StringUtils.isNotEmpty(organisms)) {
            request.setAttribute("organisms", organisms);
        }
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        final Properties webProperties = SessionMethods.getWebProperties(servletContext);
        final FriendlyMineManager linkManager = FriendlyMineManager.getInstance(im, webProperties);
        Collection<Mine> mines = linkManager.getFriendlyMines();
        request.setAttribute("mines", mines);
        return null;
    }


    private String getIdentifierField(InterMineBag bag) {
        Class c = null;
        try {
            c = Class.forName(bag.getQualifiedType());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        String identifierField = IDENTIFIER;
        // hack so we can use ensembl for ratmine
        if (TypeUtil.getFieldInfo(c, ALTERNATIVE_IDENTIFIER) != null) {
            identifierField = ALTERNATIVE_IDENTIFIER;
        }
        return identifierField;
    }
}
