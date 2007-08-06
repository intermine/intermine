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

import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.tagging.TagTypes;

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

/**
 * Tiles controller for history tile (page).
 *
 * @author Thomas Riley
 */
public class MyMineController extends TilesAction
{
    /**
     * Set up attributes for the myMine page.
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ProfileManager pm = SessionMethods.getProfileManager(servletContext);
        String page = request.getParameter("page");

        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        
        /* if the user is on a restricted page and they are not logged in, send them to the bags
         * page.  query history is not a restricted page.
         */
        if (page != null && !page.equals("history") && !profile.isLoggedIn()) {
            page = "lists";
        }
        
        if (!StringUtils.isEmpty(page)) {
            session.setAttribute(Constants.MYMINE_PAGE, page);
        }

        if (page != null) {
            if (page.equals("templates")) {
                // prime the tags cache so that the templates tags will be quick to access
                String userName = ((Profile) session.getAttribute(Constants.PROFILE)).getUsername();
                if (userName != null) {
                    // discard result
                    pm.getTags(null, null, TagTypes.TEMPLATE, userName);
                }
            }
        }
        return null;
    }
}
