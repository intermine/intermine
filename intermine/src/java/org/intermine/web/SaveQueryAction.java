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

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryCloner;
import org.intermine.objectstore.query.ResultsInfo;

import java.util.Map;

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
        
        Map savedQueries = (Map) session.getAttribute(Constants.SAVED_QUERIES);
        Map savedQueriesInverse =
            (Map) session.getAttribute(Constants.SAVED_QUERIES_INVERSE);
        
        Query query = (Query) session.getAttribute(Constants.QUERY);
        
        if (query == null) {
            return mapping.findForward("results");
        }
        
        Query clonedQuery = QueryCloner.cloneQuery(query);
        
        SaveQueryForm sqForm = (SaveQueryForm) form;
        String queryName = sqForm.getQueryName();
        sqForm.setQueryName("");
        
        ServletContext servletContext = session.getServletContext();
        
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        
        ResultsInfo resultsInfo;

        try {
            resultsInfo = os.estimate(query);
        } catch (ObjectStoreException e) {
            // no estimate to store
            resultsInfo = null;
        }
        
        SaveQueryHelper.saveQuery(request, queryName, clonedQuery, resultsInfo);

        return mapping.findForward("results");
    }
}
