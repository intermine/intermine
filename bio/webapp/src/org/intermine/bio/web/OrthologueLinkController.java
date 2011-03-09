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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

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
import org.intermine.bio.web.logic.BioUtil;
import org.intermine.util.PropertiesUtil;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.bag.BagHelper;
import org.intermine.web.logic.session.SessionMethods;


/**
 * Gets list of friendly intermines to show on list analysis page.
 *
 * @author Julie Sullivan
 */
public class OrthologueLinkController  extends TilesAction
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
        String organisms = StringUtil.join(organismsInBag, ",");
        Properties webProperties = SessionMethods.getWebProperties(request.getSession()
                .getServletContext());
        String localMineName = webProperties.getProperty("project.title");
        Properties props = PropertiesUtil.stripStart("intermines",
                PropertiesUtil.getPropertiesStartingWith("intermines", webProperties));
        Enumeration<?> propNames = props.propertyNames();
        Set<String> mines = new HashSet<String>();
        while (propNames.hasMoreElements()) {
            String mineId =  (String) propNames.nextElement();
            mineId = mineId.substring(0, mineId.indexOf("."));
            Properties mineProps = PropertiesUtil.stripStart(mineId,
                    PropertiesUtil.getPropertiesStartingWith(mineId, props));
            String mineName = mineProps.getProperty("name");
            if (StringUtils.isNotEmpty(mineName) && !mineName.equals(localMineName)) {
                mines.add(mineName);
            }
        }

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
        String identifierList = BagHelper.getIdList(bag, im.getObjectStore(), "", identifierField);
        request.setAttribute("identifierList", identifierList);

        if (!mines.isEmpty()) {
            request.setAttribute("mines", mines);
            request.setAttribute("organisms", organisms);
        }
        return null;
    }
}
