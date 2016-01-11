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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.web.logic.profile.LoginHandler;
import org.intermine.web.logic.profile.ProfileMergeIssues;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Action to handle button presses on the main tile
 *
 * @author Mark Woodbridge
 */
public class LoginAction extends LoginHandler
{
    /**
     * Method called for login in
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws an exception
     */
    @Override public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);

        LoginForm lf = (LoginForm) form;

        ActionErrors errors = lf.validate(mapping, request);
        if (!errors.isEmpty()) {
            saveErrors(request, errors);
            return mapping.findForward("login");
        }

        ProfileMergeIssues issues = doLogin(request, lf.getUsername(), lf.getPassword());
        Profile user = SessionMethods.getProfile(session);
        recordMessage(new ActionMessage("login.loggedin", user.getName()), request);
        //track the user login
        im.getTrackerDelegate().trackLogin(lf.getUsername());

        if (issues.hasIssues()) {
            for (Map.Entry<String, String> renamed: issues.getRenamedBags().entrySet()) {
                recordMessage(new ActionMessage("login.renamedbags", renamed.getKey(),
                            renamed.getValue()), request);
            }
            for (Map.Entry<String, String> renamed: issues.getRenamedTemplates().entrySet()) {
                recordMessage(
                    new ActionMessage("login.failedtemplate", renamed.getKey(), renamed.getValue()),
                    request);
            }
            return mapping.findForward("mymine");
        }

        // don't return user to the page they logged in from in certain situations
        // eg. if they were in the middle of uploading a list
        if (lf.returnToString != null && lf.returnToString.startsWith("/")
                && lf.returnToString.indexOf("error") == -1
                && lf.returnToString.indexOf("bagUploadConfirm") == -1
                && lf.returnToString.indexOf("keywordsearchresults") == -1) {
            return new ActionForward(lf.returnToString);
        }
        return mapping.findForward("mymine");
    }
}
