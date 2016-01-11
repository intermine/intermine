package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
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
import org.intermine.web.logic.results.WebState;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Controller for the submenu at the top of every page
 * @author Xavier Watkins
 */
public class SubMenuController extends TilesAction
{
    /**
     * {@inheritDoc}
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        String subtab = request.getParameter("subtab");
        String pageName = (String) request.getAttribute("pageName");
        WebState webState = SessionMethods.getWebState(request.getSession());

        if (subtab != null && subtab.length() != 0) {
            webState.addSubtab("subtab" + pageName, subtab);
        }

        request.setAttribute("subtabs", webState.getSubtabs());
        return null;
    }

}
