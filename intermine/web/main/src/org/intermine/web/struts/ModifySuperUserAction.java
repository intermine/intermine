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

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.ProfileManager;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Implementation of Action to modify superuser role
 *
 * @author Daniela Butano
 */
public class ModifySuperUserAction extends InterMineAction
{

    /**
     * Forward to the correct method based on the button pressed
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws an exception
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        ProfileManager pm = im.getProfileManager();
        List<String> allUsers = pm.getProfileUserNames();
        ModifySuperUserForm superUserForm = (ModifySuperUserForm) form;
        String[] superUsersList = superUserForm.getSuperUsers();
        if (superUsersList.length == 0) {
            recordMessage(new ActionMessage("errors.users.superusernotselected"), request);
            return mapping.findForward("mymine");
        }
        String suInProperties = pm.getSuperuser();
        boolean found = false;
        for (String superUser : superUsersList) {
            if (superUser.equals(suInProperties)) {
                found = true;
                break;
            }
        }
        if (!found) {
            recordMessage(new ActionMessage("errors.users.superuserinpropertiesnotselected"),
                         request);
        }

        boolean updated;
        for (String user : allUsers) {
            updated = false;
            for (String superUser : superUsersList) {
                if (superUser.equals(user)) {
                    pm.updateSuperUser(user, true);
                    updated = true;
                    break;
                }
            }
            if (!updated && !user.equals(suInProperties)) {
                pm.updateSuperUser(user, false);
            }
        }

        return mapping.findForward("mymine");
    }
}
