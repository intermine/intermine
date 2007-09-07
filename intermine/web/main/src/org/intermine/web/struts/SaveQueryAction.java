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

import java.util.Map;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.query.MainHelper;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.session.SessionMethods;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

/**
 * Implementation of <strong>Action</strong> that saves a Query from a session.
 *
 * @author Richard Smith
 * @author Matthew Wakeling
 */
public class SaveQueryAction extends InterMineAction
{
    protected static final Logger LOG = Logger.getLogger(SaveQueryAction.class);

    /**
     * Process the specified HTTP request, and create the corresponding HTTP
     * response (or forward to another web component that will create it).
     * Return an <code>ActionForward</code> instance describing where and how
     * control should be forwarded, or <code>null</code> if the response has
     * already been completed.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
        String queryName = ((SaveQueryForm) form).getQueryName();

        // TODO just add default fields to select list?
        if (query.getView().isEmpty()) {
             recordError(new ActionMessage("errors.query.badquery"), request);
            return mapping.findForward("query");
        }
        
        try {
            
            if (query.getInfo() == null) {
                Map<String, InterMineBag> allBags =
                    WebUtil.getAllBags(profile.getSavedBags(), servletContext);
                query.setInfo(os.estimate(MainHelper.makeQuery(query, allBags,
                                servletContext, null)));
            }
        } catch (ObjectStoreException e) {
            recordError(new ActionMessage("errors.query.objectstoreerror"), request, e, LOG);
        }
        
        SessionMethods.saveQuery(session, queryName, query);

        ActionMessages messages = (ActionMessages) request.getAttribute(Globals.MESSAGE_KEY);
        if (messages == null) {
            messages = new ActionMessages();
        }
        messages.add("saveQuery", new ActionMessage("saveQuery.message", queryName));
        request.setAttribute(Globals.MESSAGE_KEY, messages);
        
        return mapping.findForward("mymine");
    }
}
