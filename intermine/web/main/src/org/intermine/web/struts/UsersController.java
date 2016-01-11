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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.ProfileManager;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Controller for the users admin page
 * @author Daniela Butano
 */
public class UsersController extends TilesAction
{

    @Override
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        ProfileManager pm = im.getProfileManager();
        List<String> superUsersList = pm.getSuperUsers();
        String[] superUsers = superUsersList.toArray(new String[superUsersList.size()]);
        List<String> allUsers = pm.getProfileUserNames();
        request.setAttribute("users", allUsers);
        ModifySuperUserForm superUserForm = (ModifySuperUserForm) form;
        superUserForm.setSuperUsers(superUsers);
        return null;
    }

}
