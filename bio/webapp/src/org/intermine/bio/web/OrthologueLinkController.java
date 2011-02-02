package org.intermine.bio.web;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.mines.HomologueMapping;
import org.intermine.api.mines.Mine;
import org.intermine.api.mines.OrthologueLinkManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.bio.web.logic.BioUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.bag.BagHelper;
import org.intermine.web.logic.session.SessionMethods;


/**
 * Class that generates links to other intermines
 * @author julie
 */
public class OrthologueLinkController  extends TilesAction
{
    private String identifierField = "primaryIdentifier";
    private String alternativeIdentifierField = "ensemblIdentifier";

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) {

        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        InterMineBag bag = (InterMineBag) request.getAttribute("bag");
        Properties webProperties = SessionMethods.getWebProperties(request.getSession()
                .getServletContext());
        Class c = null;
        try {
            c = Class.forName(bag.getQualifiedType());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        // hack so we can use ensembl for ratmine
        if (TypeUtil.getFieldInfo(c, alternativeIdentifierField) != null) {
            identifierField = alternativeIdentifierField;
        }
        String identifierList = BagHelper.getIdList(bag, im.getObjectStore(), "", identifierField);
        request.setAttribute("identifierList", identifierList);

        OrthologueLinkManager olm = OrthologueLinkManager.getInstance(im, webProperties);
        Collection<String> organismNamesInBag = BioUtil.getOrganisms(im.getObjectStore(), bag,
                false, "shortName");
        Map<Mine, Map<String, HomologueMapping>> mines = olm.getMines(organismNamesInBag);
        if (!mines.isEmpty()) {
            request.setAttribute("mines", mines);
        }
        return null;
    }
}
