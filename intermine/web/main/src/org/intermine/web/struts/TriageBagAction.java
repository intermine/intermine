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

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.profile.BagValue;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.StorableBag;
import org.intermine.web.logic.session.SessionMethods;

/**
 * An action to handle reparative actions to bags.
 * @author Alex Kalderimis
 *
 */
public class TriageBagAction extends InterMineAction
{
    private static final Logger LOG = Logger.getLogger(TriageBagAction.class);

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
        TriageBagForm tbf = (TriageBagForm) form;
        String[] selectedBagNames = tbf.getSelectedBags();
        HttpSession session = request.getSession();
        Profile profile = SessionMethods.getProfile(session);

        // This should already be caught by Ajax code
        if (selectedBagNames.length == 0) {
            recordError(new ActionMessage("errors.bag.listnotselected"), request);
            return getReturn(tbf.getPageName(), mapping);
        }

        if (request.getParameter("export") != null
                || (tbf.getListsButton() != null && "export".equals(tbf.getListsButton()))) {
            String bagName = getBagName(tbf.getSelectedBags());
            response.setContentType("text/plain");
            response.setHeader("Content-Disposition ", "attachment; filename=" + bagName + ".tsv");
            PrintWriter writer = response.getWriter();
            for (String name: tbf.getSelectedBags()) {
                StorableBag s = profile.getAllBags().get(name);
                writer.println("# CONTENTS OF: " + name + " (" + s.getDescription() + ")");
                for (BagValue bv: s.getContents()) {
                    writer.println(bv.getValue() + "\t" + bv.getExtra());
                }
            }
            writer.flush();
            return null;
        } else if (request.getParameter("delete") != null
                || (tbf.getListsButton() != null && "delete".equals(tbf.getListsButton()))) {
            for (String name: tbf.getSelectedBags()) {
                profile.deleteBag(name);
            }
        } else {
            LOG.error("Nothing done! listsButton='" + tbf.getListsButton() + "'");
        }

        return getReturn(tbf.getPageName(), mapping);
    }

    private String getBagName(String[] names) {
        if (names.length == 1) {
            return names[0];
        }
        // TODO: Mangle names to remove spaces
        // TODO: Handle 0 names.
        return StringUtils.join(names, "+");
    }

    private ActionForward getReturn(String pageName, ActionMapping mapping) {
        if (pageName != null && "MyMine".equals(pageName)) {
            return new ForwardParameters(mapping.findForward("mymine"))
                    .addParameter("subtab", "lists").forward();
        }
        return new ForwardParameters(mapping.findForward("bag"))
                    .addParameter("subtab", "view").forward();
    }
}
