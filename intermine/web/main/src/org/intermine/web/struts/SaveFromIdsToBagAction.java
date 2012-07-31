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

import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.util.NameUtil;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Saves selected items with InterMine ids and a Type in a new bag or combines
 * with existing bag.
 *
 * @author Fengyuan Hu
 */
public class SaveFromIdsToBagAction extends InterMineAction
{
    protected static final Logger LOG = Logger
            .getLogger(SaveFromIdsToBagAction.class);

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
        throws Exception {

        HttpSession session = request.getSession();
        Profile profile = SessionMethods.getProfile(session);

        // where the request comes from, e.g. /experiment.do?...
        String source = (String) request.getParameter("source");

        try {
            String type = (String) request.getParameter("type");

            String[] idArray = request.getParameter("ids").split(","); // ids are comma delimited

            Set<Integer> idSet = new LinkedHashSet<Integer>();
            for (String id : idArray) {
                idSet.add(Integer.valueOf(id.trim()));
            }

            String bagName = request.getParameter("newBagName");
            if (bagName == null) {
                bagName = "new_list";
            }
            bagName = NameUtil.generateNewName(profile.getSavedBags().keySet(),
                    bagName);

            InterMineAPI im = SessionMethods.getInterMineAPI(session);
            InterMineBag bag = profile.createBag(bagName, type, "", im.getClassKeys());
            bag.addIdsToBag(idSet, type);

            profile.saveBag(bag.getName(), bag);

            ForwardParameters forwardParameters = new ForwardParameters(
                    mapping.findForward("bagDetails"));
            return forwardParameters.addParameter("bagName", bagName).forward();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                recordError(new ActionMessage(e.toString()), request);

                ActionForward actionForward = mapping.findForward("bagDetails");
                ActionForward newActionForward = new ActionForward(
                        actionForward);
                if (request.getQueryString() == null) {
                    newActionForward.setPath("/" + source + ".do");
                } else {
                    newActionForward.setPath("/" + source + ".do?" + request.getQueryString());
                }

                return newActionForward;
            } catch (Exception ex) {
                ex.printStackTrace();
                recordError(new ActionMessage("Error...Please report..."),
                        request);

                return mapping.findForward("begin");
            }
        }
    }
}
