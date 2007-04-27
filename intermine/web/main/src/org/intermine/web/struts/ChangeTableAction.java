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

import org.intermine.web.logic.results.PageOutOfRangeException;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

/**
 * Implementation of <strong>DispatchAction</strong>. Changes the
 * view of the results in some way.
 *
 * @author Andrew Varley
 * @author Thomas Riley
 */
public class ChangeTableAction extends InterMineDispatchAction
{
    /**
     * Change to the last results page
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     */
    public ActionForward last(ActionMapping mapping, 
                              @SuppressWarnings("unused") ActionForm form,
                              HttpServletRequest request, 
                              @SuppressWarnings("unused") HttpServletResponse response) {
        HttpSession session = request.getSession();
        PagedTable pt = SessionMethods.getResultsTable(session, request.getParameter("table"));

        int page = ((pt.getExactSize() - 1) / pt.getPageSize());
        try {
            pt.setPageAndPageSize(page, pt.getPageSize());
        } catch (PageOutOfRangeException e) {
            recordError(new ActionMessage("results.maxoffsetreached"), request);
        }

        return makeResultsForward(mapping.findForward("results"), request, pt);
    }

    /**
     * Hide a column
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     */
    public ActionForward hideColumn(ActionMapping mapping, 
                                    @SuppressWarnings("unused") ActionForm form,
                                    HttpServletRequest request, 
                                    @SuppressWarnings("unused") HttpServletResponse response) {
        HttpSession session = request.getSession();
        PagedTable pt = SessionMethods.getResultsTable(session, request.getParameter("table"));
        
        int index = Integer.parseInt(request.getParameter("index"));
        pt.getColumns().get(index).setVisible(false);

        return makeResultsForward(mapping.findForward("results"), request, pt);
    }

    /**
     * Show a column
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     */
    public ActionForward showColumn(ActionMapping mapping, 
                                    @SuppressWarnings("unused") ActionForm form,
                                    HttpServletRequest request, 
                                    @SuppressWarnings("unused") HttpServletResponse response) {
        HttpSession session = request.getSession();
        PagedTable pt = SessionMethods.getResultsTable(session, request.getParameter("table"));

        int index = Integer.parseInt(request.getParameter("index"));
        pt.getColumns().get(index).setVisible(true);

        return makeResultsForward(mapping.findForward("results"), request, pt);
    }

    /**
     * Move a column left
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     */
    public ActionForward moveColumnLeft(ActionMapping mapping, 
                                        @SuppressWarnings("unused") ActionForm form,
                                        HttpServletRequest request, 
                                        @SuppressWarnings("unused") HttpServletResponse response) {
        HttpSession session = request.getSession();
        PagedTable pt = SessionMethods.getResultsTable(session, request.getParameter("table"));

        int index = Integer.parseInt(request.getParameter("index"));
        pt.moveColumnLeft(index);

        return makeResultsForward(mapping.findForward("results"), request, pt);
    }

    /**
     * Move a column right
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     */
    public ActionForward moveColumnRight(ActionMapping mapping, 
                                         @SuppressWarnings("unused") ActionForm form,
                                         HttpServletRequest request,
                                         @SuppressWarnings("unused") HttpServletResponse response) {
        HttpSession session = request.getSession();
        PagedTable pt = SessionMethods.getResultsTable(session, request.getParameter("table"));

        int index = Integer.parseInt(request.getParameter("index"));
        pt.moveColumnRight(index);

        return makeResultsForward(mapping.findForward("results"), request, pt);
    }
    
    /**
     * Create a forward with parameters setting start item and page size.
     * 
     * @param results ActionForward to results action
     * @param request the current HttpServletRequest
     * @param pt PagedTable
     * @return an ActionForward with parameters
     */
    protected ActionForward makeResultsForward(ActionForward results, HttpServletRequest request,
                                                             PagedTable pt) {
        ForwardParameters forward = new ForwardParameters(results)
                .addParameter("table", request.getParameter("table"))
                .addParameter("page", "" + pt.getPage())
                .addParameter("size", "" + pt.getPageSize());
        if (request.getParameter("trail") != null) {
            forward.addParameter("trail", request.getParameter("trail"));
        }
        return forward.forward();
    }
}
