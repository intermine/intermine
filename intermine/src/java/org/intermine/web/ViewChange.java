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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.List;

import org.apache.struts.actions.DispatchAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.Globals;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreQueryDurationException;

import org.intermine.web.results.ChangeResultsForm;

/**
 * Action to handle links on view tile
 * @author Mark Woodbridge
 */
public class ViewChange extends DispatchAction
{
    /**
     * Remove a Node from the results view
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     */
    public ActionForward removeFromView(ActionMapping mapping,
                                        ActionForm form,
                                        HttpServletRequest request,
                                        HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        List view = (List) session.getAttribute(Constants.VIEW);
        String path = request.getParameter("path");

        view.remove(path);

        return mapping.findForward("query");
    }

    /**
     * Shift a Node left in the view
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     */
    public ActionForward moveLeft(ActionMapping mapping,
                                  ActionForm form,
                                  HttpServletRequest request,
                                  HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        List view = (List) session.getAttribute(Constants.VIEW);
        int index = Integer.parseInt(request.getParameter("index"));

        Object o = view.get(index - 1);
        view.set(index - 1, view.get(index));
        view.set(index, o);

        return mapping.findForward("query");
    }

    /**
     * Shift a Node right in the view
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     */
    public ActionForward moveRight(ActionMapping mapping,
                                   ActionForm form,
                                   HttpServletRequest request,
                                   HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        List view = (List) session.getAttribute(Constants.VIEW);
        int index = Integer.parseInt(request.getParameter("index"));

        Object o = view.get(index + 1);
        view.set(index + 1, view.get(index));
        view.set(index, o);

        return mapping.findForward("query");
    }

    /**
     * Run the query and forward to the results page.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward runQuery(ActionMapping mapping,
                                  ActionForm form,
                                  HttpServletRequest request,
                                  HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();

        ChangeResultsForm resultsForm =
            (ChangeResultsForm) session.getAttribute("changeResultsForm");

        if (resultsForm != null) {
            resultsForm.reset(mapping, request);
        }

        try {
            session.setAttribute(Constants.RESULTS_TABLE, ViewHelper.runQuery(request));
        } catch (ObjectStoreException e) {
            ActionErrors errors = (ActionErrors) request.getAttribute(Globals.ERROR_KEY);
            if (errors == null) {
                errors = new ActionErrors();
                request.setAttribute(Globals.ERROR_KEY, errors);
            }
            String key = (e instanceof ObjectStoreQueryDurationException)
                ? "errors.query.estimatetimetoolong"
                : "errors.query.objectstoreerror";
            errors.add(ActionErrors.GLOBAL_ERROR, new ActionError(key));
            return mapping.findForward("query");
        }
        
        return mapping.findForward("results");
    }
}
