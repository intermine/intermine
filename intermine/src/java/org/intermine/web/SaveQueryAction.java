package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.Globals;

import org.intermine.metadata.Model;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStore;

/**
 * Implementation of <strong>Action</strong> that saves a Query from a session.
 *
 * @author Richard Smith
 * @author Matthew Wakeling
 */
public class SaveQueryAction extends Action
{
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
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        Map qNodes = (Map) session.getAttribute(Constants.QUERY);
        List view = (List) session.getAttribute(Constants.VIEW);
        String queryName = ((SaveQueryForm) form).getQueryName();

        try {
            ResultsInfo resultsInfo = ViewHelper.makeEstimate(request);
            saveQuery(request, queryName, qNodes, view, resultsInfo);
        } catch (ObjectStoreException e) {
            ActionErrors actionErrors = new ActionErrors();
            actionErrors.add(ActionErrors.GLOBAL_ERROR,
                               new ActionError("errors.query.objectstoreerror"));
            saveErrors(request, actionErrors);
        }

        return mapping.findForward("query");
    }

    /**
     * Save a query in the Map on the session, and clone it to allow further editing
     * @param request The HTTP request we are processing
     * @param queryName the name to save the query under
     * @param qNodes the actual query
     * @param view the paths in the SELECT list
     * @param resultsInfo the resultsInfo for the query
     */
    public static void saveQuery(HttpServletRequest request,
                                 String queryName,
                                 Map qNodes,
                                 List view,
                                 ResultsInfo resultsInfo) {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Model model = (Model) os.getModel();
        Map savedQueries = (Map) session.getAttribute(Constants.SAVED_QUERIES);

        if (savedQueries == null) {
            savedQueries = new LinkedHashMap();
            session.setAttribute(Constants.SAVED_QUERIES, savedQueries);
        }
        
        savedQueries.put(queryName, new QueryInfo(qNodes, view, resultsInfo));
        
        session.setAttribute(Constants.QUERY, SaveQueryHelper.clone(qNodes, model));
        session.setAttribute(Constants.VIEW, new ArrayList(view));

        ActionMessages messages = (ActionMessages) request.getAttribute(Globals.MESSAGE_KEY);
        if (messages == null) {
            messages = new ActionMessages();
        }
        messages.add("saveQuery", new ActionMessage("saveQuery.message", queryName));
        request.setAttribute(Globals.MESSAGE_KEY, messages);
    }
}
