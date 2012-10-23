package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.ProfileManager;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Controller for the password reset page.
 *
 * @author Matthew Wakeling
 */
public class PasswordResetController extends TilesAction
{
    /**
     * Set up the form.
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        String token = request.getParameter("token");
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        if (token == null) {
            token = (String) session.getAttribute("passwordResetToken");
        } else {
            session.setAttribute("passwordResetToken", token);
        }
        ProfileManager pm = im.getProfileManager();
        try {
            String username = pm.getUsernameForToken(token);
            request.setAttribute("IS_VALID", true);
            request.setAttribute("username", username);
        } catch (IllegalArgumentException e) {
            // Invalid token, so do nothing
        }
        return null;
    }
}
