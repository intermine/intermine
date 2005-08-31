package org.intermine.web.history;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.intermine.web.Constants;
import org.intermine.web.ForwardParameters;
import org.intermine.web.InterMineAction;
import org.intermine.web.Profile;
import org.intermine.web.SavedQuery;
import org.intermine.web.bag.InterMineBag;

import org.apache.log4j.Logger;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

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
            profile.saveBag(newName, bag);
        } else {
            LOG.error("Don't understand type parameter: " + type);
        }

        return mapping.findForward("history");
    }
    
    /**
     * 
     * @param history
     * @param type
     * @param name
     * @return
     */
    protected ActionForward editForward(ActionForward history, String type, String name) {
        return new ForwardParameters(history).addParameter("action", "rename")
            .addParameter("type", type).addParameter("name", name).forward();
    }
}
