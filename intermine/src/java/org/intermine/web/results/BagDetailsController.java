package org.intermine.web.results;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;
import org.apache.struts.tiles.ComponentContext;

import org.intermine.web.Constants;
import org.intermine.web.Profile;
import org.intermine.web.InterMineBag;

/**
 * Implementation of <strong>TilesAction</strong>. Assembles data for
 * viewing a bag.
 *
 * @author Kim Rutherford
 */
public class BagDetailsController extends TilesAction
{
    /**
     * Set up session attributes for the bag details page.
     *
     * @param context The Tiles ComponentContext
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if an error occurs
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        
        String bagName = request.getParameter("bagName");
        InterMineBag bag = (InterMineBag) profile.getSavedBags().get(bagName);

        if (bag == null) {
            bag = new InterMineBag();
        }

        request.setAttribute("bagName", bagName);
        session.setAttribute(Constants.RESULTS_TABLE, new PagedCollection(bagName, bag));

        return null;
    }
}
