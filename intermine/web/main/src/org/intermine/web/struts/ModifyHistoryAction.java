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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.ForwardParameters;
import org.intermine.web.logic.Profile;
import org.intermine.web.logic.SavedQuery;
import org.intermine.web.logic.SessionMethods;
import org.intermine.web.logic.bag.InterMineBag;

/**
 * Actions for links on the history page. There are bag and query specific
 * subclasses.
 * 
 * @author Thomas Riley
 */
public abstract class ModifyHistoryAction extends InterMineAction
{
    private static final Logger LOG = Logger.getLogger(ModifyHistoryAction.class);

    /**
     * Forward to the correct method based on the button pressed.
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        if (request.getParameter("newName") != null) {
            return rename(mapping, form, request, response);
        } else {
            LOG.debug("execute returning null");
            return null;
        }
    }
    
    /**
     * Rename a bag or query.
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward rename(ActionMapping mapping,
                                ActionForm form,
                                HttpServletRequest request,
                                HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        String type = request.getParameter("type");
        String name = request.getParameter("name");
        String newName = request.getParameter("newName");
        ActionForward historyForward = mapping.findForward("history");
        SavedQuery sq;

        if (name.equals(newName)) { 
            return historyForward; 
        } 
        if (StringUtils.isEmpty(newName)) {
            recordError(new ActionMessage("errors.required", "New name"), request);
            return editForward(historyForward, type, name);
        }
        
        if (type.equals("history")) {
            if (profile.getHistory().get(newName) != null) {
                recordError(new ActionMessage("errors.modifyQuery.queryExists", newName), request);
                return editForward(historyForward, type, name);
            }
            profile.renameHistory(name, newName);
        } else if (type.equals("saved")) {
            if (profile.getSavedQueries().get(newName) != null) {
                recordError(new ActionMessage("errors.modifyQuery.queryExists", newName), request);
                return editForward(historyForward, type, name);
            }
            sq = (SavedQuery) profile.getSavedQueries().get(name);
            profile.deleteQuery(sq.getName());
            sq = new SavedQuery(newName, sq.getDateCreated(), sq.getPathQuery());
            profile.saveQuery(sq.getName(), sq);
        } else if (type.equals("bag")) {
            if (profile.getSavedBags().get(newName) != null) {
                recordError(new ActionMessage("errors.modifyQuery.queryExists", newName), request);
                return editForward(historyForward, type, name);
            }
            InterMineBag bag = (InterMineBag) profile.getSavedBags().get(name);
            profile.deleteBag(name);
            SessionMethods.invalidateBagTable(session, name);
            profile.saveBag(newName, bag, Constants.MAX_NOT_LOGGED_BAG_SIZE);
        } else {
            LOG.error("Don't understand type parameter: " + type);
        }

        return mapping.findForward("history");
    }
    
    /**
     * Construct an ActionForward that redirects the user to edit particular item
     * on the history page.
     * @param history basic ActionForward to history page
     * @param type type of thing to rename (save/history/bag)
     * @param name name of thing to rename
     * @return ActionForward to name edit
     */
    protected ActionForward editForward(ActionForward history, String type, String name) {
        return new ForwardParameters(history).addParameter("action", "rename")
            .addParameter("type", type).addParameter("name", name).forward();
    }
}
