package org.flymine.web.results;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import org.apache.struts.actions.DispatchAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryNode;
import org.flymine.objectstore.query.Results;
import org.flymine.objectstore.query.ResultsRow;
import org.flymine.web.Constants;

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
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception ServletException if a servlet error occurs
     */
    public ActionForward next(ActionMapping mapping, ActionForm form,
                              HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        HttpSession session = request.getSession();

        DisplayableResults dr = (DisplayableResults) session.getAttribute(Constants.RESULTS_TABLE);

        int prevStart = dr.getStart();
        int pageSize = dr.getPageSize();

        dr.setStart(prevStart + pageSize);

        return mapping.findForward("results");
    }

    /**
     * Change to the previous results page
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception ServletException if a servlet error occurs
     */
    public ActionForward previous(ActionMapping mapping, ActionForm form,
                                  HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        HttpSession session = request.getSession();

        DisplayableResults dr = (DisplayableResults) session.getAttribute(Constants.RESULTS_TABLE);

        int prevStart = dr.getStart();
        int pageSize = dr.getPageSize();

        int newStart = prevStart - pageSize;
        if (newStart < 0) {
            newStart = 0;
        }

        dr.setStart(newStart);

        return mapping.findForward("results");
    }

    /**
     * Change to the first results page
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception ServletException if a servlet error occurs
     */
    public ActionForward first(ActionMapping mapping, ActionForm form,
                                  HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        HttpSession session = request.getSession();

        DisplayableResults dr = (DisplayableResults) session.getAttribute(Constants.RESULTS_TABLE);
        dr.setStart(0);

        return mapping.findForward("results");
    }

    /**
     * Change to the last results page
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception ServletException if a servlet error occurs
     */
    public ActionForward last(ActionMapping mapping, ActionForm form,
                              HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        HttpSession session = request.getSession();

        DisplayableResults dr = (DisplayableResults) session.getAttribute(Constants.RESULTS_TABLE);

        int pageSize = dr.getPageSize();

        // Here we have to force the results to give us an exact size
        int size = dr.getResults().size();
        int start = ((size - 1) / pageSize) * pageSize;

        if (start < 0) {
            start = 0;
        }
        dr.setStart(start);

        return mapping.findForward("results");
    }

    /**
     * Hide a column. Must pass in a parameter "columnAlias" to
     * indicate the column being hidden.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception ServletException if a servlet error occurs
     */
    public ActionForward hideColumn(ActionMapping mapping, ActionForm form,
                              HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        HttpSession session = request.getSession();

        DisplayableResults dr = (DisplayableResults) session.getAttribute(Constants.RESULTS_TABLE);

        String columnAlias = request.getParameter("columnAlias");
        if (columnAlias == null) {
            throw new IllegalArgumentException("A columnAlias parameter must be present");
        }

        dr.getColumn(columnAlias).setVisible(false);

        return mapping.findForward("results");
    }

    /**
     * Show a column. Must pass in a parameter "columnAlias" to
     * indicate the column being shown.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception ServletException if a servlet error occurs
     */
    public ActionForward showColumn(ActionMapping mapping, ActionForm form,
                              HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        HttpSession session = request.getSession();

        DisplayableResults dr = (DisplayableResults) session.getAttribute(Constants.RESULTS_TABLE);

        String columnAlias = request.getParameter("columnAlias");
        if (columnAlias == null) {
            throw new IllegalArgumentException("A columnAlias parameter must be present");
        }

        dr.getColumn(columnAlias).setVisible(true);

        return mapping.findForward("results");
    }

    /**
     * Move a column nearer the top of the list of columns. Must pass
     * in a parameter "columnAlias" to indicate the column being
     * moved.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception ServletException if a servlet error occurs
     */
    public ActionForward moveColumnUp(ActionMapping mapping, ActionForm form,
                                      HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        HttpSession session = request.getSession();

        DisplayableResults dr = (DisplayableResults) session.getAttribute(Constants.RESULTS_TABLE);

        String columnAlias = request.getParameter("columnAlias");
        if (columnAlias == null) {
            throw new IllegalArgumentException("A columnAlias parameter must be present");
        }

        dr.moveColumnUp(columnAlias);
        return mapping.findForward("results");
    }

    /**
     * Move a column nearer to the bottom of the list of columns. Must
     * pass in a parameter "columnAlias" to indicate the column being
     * moved.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception ServletException if a servlet error occurs
     */
    public ActionForward moveColumnDown(ActionMapping mapping, ActionForm form,
                                        HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        HttpSession session = request.getSession();

        DisplayableResults dr = (DisplayableResults) session.getAttribute(Constants.RESULTS_TABLE);

        String columnAlias = request.getParameter("columnAlias");
        if (columnAlias == null) {
            throw new IllegalArgumentException("A columnAlias parameter must be present");
        }

        dr.moveColumnDown(columnAlias);
        return mapping.findForward("results");
    }

    /**
     * Order by a particular column. Must pass in a parameter
     * "columnAlias" to indicate the column being ordered by.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception ServletException if a servlet error occurs
     */
    public ActionForward orderByColumn(ActionMapping mapping, ActionForm form,
                                       HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        HttpSession session = request.getSession();

        DisplayableResults dr = (DisplayableResults) session.getAttribute(Constants.RESULTS_TABLE);

        Query q = dr.getResults().getQuery();

        String columnAlias = request.getParameter("columnAlias");
        if (columnAlias == null) {
            throw new IllegalArgumentException("A columnAlias parameter must be present");
        }

        // For now, we will delete everything that exists in the order
        // by and then add the QueryNode corresponding to the
        // columnAlias. This might not be a sensible long-term solution
        // but will do for now
        List nodes = new LinkedList();
        nodes.addAll(q.getOrderBy());
        Iterator nodesIter = nodes.iterator();
        while (nodesIter.hasNext()) {
            QueryNode qn = (QueryNode) nodesIter.next();
            q.deleteFromOrderBy(qn);
        }

        q.addToOrderBy((QueryNode) q.getReverseAliases().get(columnAlias));

        session.setAttribute("query", q);
        return mapping.findForward("runquery");
    }

    /**
     * Change to the next results page
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception ServletException if a servlet error occurs
     */
    public ActionForward details(ActionMapping mapping, ActionForm form,
                              HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        HttpSession session = request.getSession();

        DisplayableResults dr = (DisplayableResults) session.getAttribute(Constants.RESULTS_TABLE);

        Results results = dr.getResults();
        String column = request.getParameter("columnIndex");
        if (column == null) {
            throw new IllegalArgumentException("A column parameter must be present");
        }
        String row = request.getParameter("rowIndex");
        if (row == null) {
            throw new IllegalArgumentException("A row parameter must be present");
        }

        Object obj = ((ResultsRow) results.get(Integer.parseInt(row)))
            .get(Integer.parseInt(column));

        request.setAttribute("object", obj);

        return mapping.findForward("details");
    }



}
