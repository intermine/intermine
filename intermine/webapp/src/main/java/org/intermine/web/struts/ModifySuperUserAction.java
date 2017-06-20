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

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.search.GlobalRepository;
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
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        ProfileManager pm = im.getProfileManager();
        List<String> allUsers = pm.getProfileUserNames();
        ModifySuperUserForm superUserForm = (ModifySuperUserForm) form;
        String[] superUsers = superUserForm.getSuperUsers();
        List<String> superUsersList = Arrays.asList(superUsers);
        //some checks
        if (superUsersList.isEmpty()) {
            recordMessage(new ActionMessage("errors.users.superusernotselected"), request);
            return mapping.findForward("mymine");
        }
        String suInProperties = pm.getSuperuser();
        if (!superUsersList.contains(suInProperties)) {
            recordMessage(new ActionMessage("errors.users.superuserinpropertiesnotselected"),
                         request);
        }
        Profile profileLogged = SessionMethods.getProfile(session);
        String userLogged = profileLogged.getUsername();
        if (!superUsersList.contains(userLogged)) {
            recordMessage(new ActionMessage("errors.users.userloggednotselected"),
                         request);
        }

        Profile profileToUpdate;
        for (String user : allUsers) {
            profileToUpdate = pm.getProfile(user);
            if (superUsersList.contains(user)) {
                if (!profileToUpdate.isSuperuser()) {
                    profileToUpdate.setSuperuser(true);
                    SessionMethods.setGlobalSearchRepository(session.getServletContext(),
                        new GlobalRepository(profileToUpdate));
                }
            } else {
                if (!user.equals(suInProperties) && !user.equals(userLogged)) {
                    if (profileToUpdate.isSuperuser()) {
                        profileToUpdate.setSuperuser(false);
                        GlobalRepository globalRepository = (GlobalRepository) SessionMethods
                            .getGlobalSearchRepository(session.getServletContext());
                        globalRepository.deleteGlobalRepository(profileToUpdate);
                    }
                }
            }
        }

        return mapping.findForward("mymine");
    }
}
