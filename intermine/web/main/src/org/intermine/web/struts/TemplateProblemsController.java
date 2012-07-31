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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.template.TemplateManager;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Called before templateProblems.jsp
 *
 * @author Fengyuan Hu
 *
 */
public class TemplateProblemsController extends TilesAction
{
    /**
     * Called before template problem page is rendered.
     *
     * {@inheritDoc}
     */
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
        throws Exception {

        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Profile pf = SessionMethods.getProfile(session);
        TemplateManager tm = im.getTemplateManager();

        String name = request.getParameter("name");
        String scope = request.getParameter("scope");

        List<String> problems;

        if ("saved".equals(scope)) {
            problems = pf.getSavedQueries().get(name).getPathQuery().verifyQuery();
        } else { // global or user
            problems = tm.getTemplate(pf, name, scope).verifyQuery();
        }

        request.setAttribute("problems", problems);

        return null;
    }
}
