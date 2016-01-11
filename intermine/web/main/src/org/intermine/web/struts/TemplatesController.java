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
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.profile.Profile;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Tiles controller for templates tile (page).
 *
 * @author Xavier Watkins
 */
public class TemplatesController extends TilesAction
{
    /**
     * Set up attributes for the templates page.
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        session.removeAttribute(Constants.NEW_TEMPLATE);
        Profile profile = SessionMethods.getProfile(session);
        MyMineController.getPrecomputedSummarisedInfo(profile, session, request);
        return null;
    }

}
