package org.intermine.web.results;

/*
 * Copyright (C) 2002-2005 FlyMine
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
import javax.servlet.ServletException;

import org.apache.struts.actions.DispatchAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.intermine.web.Constants;

/**
 * Implementation of <strong>DispatchAction</strong>. Changes the
 * view of the results in some way.
 *
 * @author Andrew Varley
 */
public class ChangeResultsAction extends DispatchAction
{
    /**
     * Change to the next results page
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception ServletException if a servlet error occurs
     */
    public ActionForward next(ActionMapping mapping, ActionForm form,
                              HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        HttpSession session = request.getSession();
        PagedTable pt = (PagedTable) session.getAttribute(Constants.RESULTS_TABLE);

        pt.nextPage();

        return mapping.findForward("results");
    }

    /**
     * Change to the previous results page
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception ServletException if a servlet error occurs
     */
    public ActionForward previous(ActionMapping mapping, ActionForm form,
                                  HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        HttpSession session = request.getSession();
        PagedTable pt = (PagedTable) session.getAttribute(Constants.RESULTS_TABLE);

        pt.previousPage();

        return mapping.findForward("results");
    }

    /**
     * Change to the first results page
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception ServletException if a servlet error occurs
     */
    public ActionForward first(ActionMapping mapping, ActionForm form,
                               HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        HttpSession session = request.getSession();
        PagedTable pt = (PagedTable) session.getAttribute(Constants.RESULTS_TABLE);

        pt.firstPage();

        return mapping.findForward("results");
    }

    /**
     * Change to the last results page
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception ServletException if a servlet error occurs
     */
    public ActionForward last(ActionMapping mapping, ActionForm form,
                              HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        HttpSession session = request.getSession();
        PagedTable pt = (PagedTable) session.getAttribute(Constants.RESULTS_TABLE);

        pt.lastPage();

        return mapping.findForward("results");
    }

    /**
     * Hide a column
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception ServletException if a servlet error occurs
     */
    public ActionForward hideColumn(ActionMapping mapping, ActionForm form,
                              HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        HttpSession session = request.getSession();
        PagedTable pt = (PagedTable) session.getAttribute(Constants.RESULTS_TABLE);
        
        int index = Integer.parseInt(request.getParameter("index"));
        ((Column) pt.getColumns().get(index)).setVisible(false);

        return mapping.findForward("results");
    }

    /**
     * Show a column
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception ServletException if a servlet error occurs
     */
    public ActionForward showColumn(ActionMapping mapping, ActionForm form,
                              HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        HttpSession session = request.getSession();
        PagedTable pt = (PagedTable) session.getAttribute(Constants.RESULTS_TABLE);

        int index = Integer.parseInt(request.getParameter("index"));
        ((Column) pt.getColumns().get(index)).setVisible(true);

        return mapping.findForward("results");
    }

    /**
     * Move a column left
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception ServletException if a servlet error occurs
     */
    public ActionForward moveColumnLeft(ActionMapping mapping, ActionForm form,
                                      HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        HttpSession session = request.getSession();
        PagedTable pt = (PagedTable) session.getAttribute(Constants.RESULTS_TABLE);

        int index = Integer.parseInt(request.getParameter("index"));
        pt.moveColumnLeft(index);

        return mapping.findForward("results");
    }

    /**
     * Move a column right
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception ServletException if a servlet error occurs
     */
    public ActionForward moveColumnRight(ActionMapping mapping, ActionForm form,
                                        HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        HttpSession session = request.getSession();
        PagedTable pt = (PagedTable) session.getAttribute(Constants.RESULTS_TABLE);

        int index = Integer.parseInt(request.getParameter("index"));
        pt.moveColumnRight(index);

        return mapping.findForward("results");
    }

    /**
     * Return a user to the results of the last query that was run
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception ServletException if a servlet error occurs
     */
    public ActionForward reset(ActionMapping mapping, ActionForm form,
                               HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        HttpSession session = request.getSession();
        session.setAttribute(Constants.RESULTS_TABLE,
                             session.getAttribute(Constants.QUERY_RESULTS));
        //SessionMethods.getObjectDetailsTrail(session).reset();
        return mapping.findForward("results");
    }
}
