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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.Profile;

/**
 * Controller for the submenu at the top of every page
 * @author Xavier Watkins
 */
public class SubMenuController extends TilesAction
{
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        String subtab = request.getParameter("subtab");
        String pageName = (String) request.getAttribute("pageName");
        Profile profile = (Profile) request.getSession().getAttribute(Constants.PROFILE);
        
        if (subtab != null && subtab.length() != 0) {
            profile.setUserOption("subtab" + pageName, subtab);
        }
        
        request.setAttribute("userOptionMap", profile.getUserOptionsMap());
        return null;
    }

}
