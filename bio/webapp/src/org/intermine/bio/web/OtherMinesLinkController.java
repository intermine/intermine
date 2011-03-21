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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.model.InterMineObject;
import org.intermine.util.DynamicUtil;
import org.intermine.util.PropertiesUtil;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Show Other Mines Links only on "Gene" page
 *
 * @author radek
 *
 */
public class OtherMinesLinkController extends TilesAction
{
    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response) {

        // we only want to display the links on a Gene Report Page
        InterMineObject imObj = (InterMineObject) request.getAttribute("object");
        if ("Gene".equals(DynamicUtil.getSimpleClass(imObj.getClass()).getSimpleName())) {
            // fetch the links to use when prepending target to other mines
            Properties webProperties = SessionMethods.getWebProperties(request.getSession()
                    .getServletContext());
            Properties props = PropertiesUtil.stripStart("intermines",
                    PropertiesUtil.getPropertiesStartingWith("intermines", webProperties));
            Enumeration<?> propNames = props.propertyNames();

            HashMap<String, LinkedHashMap<String, String>> minePortals =
                new HashMap<String, LinkedHashMap<String, String>>();
            while (propNames.hasMoreElements()) {
                String mineId =  (String) propNames.nextElement();
                mineId = mineId.substring(0, mineId.indexOf("."));
                Properties mineProps = PropertiesUtil.stripStart(mineId,
                        PropertiesUtil.getPropertiesStartingWith(mineId, props));

                // get name and url
                String mineName = mineProps.getProperty("name");
                String mineURL = mineProps.getProperty("url");
                if (StringUtils.isNotEmpty(mineURL) && StringUtils.isNotEmpty(mineName)) {
                    LinkedHashMap<String, String> mineDetails = new LinkedHashMap<String, String>();
                    // colors for the mines
                    String mineBgColor = mineProps.getProperty("bgcolor");
                    String mineFrontColor = mineProps.getProperty("frontcolor");
                    if (StringUtils.isNotEmpty(mineBgColor)
                            && StringUtils.isNotEmpty(mineFrontColor)) {
                        mineDetails.put("bgcolor", mineBgColor);
                        mineDetails.put("frontcolor", mineFrontColor);
                    }
                    mineDetails.put("url", mineURL);
                    minePortals.put(mineName, mineDetails);
                }
            }

            request.setAttribute("minePortals", minePortals);
        }

        return null;
    }
}
