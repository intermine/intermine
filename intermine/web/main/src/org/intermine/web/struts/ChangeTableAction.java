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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

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
        PagedTable pt = getPagedTable(request);

        int page = ((pt.getExactSize() - 1) / pt.getPageSize());
        pt.setPageAndPageSize(page, pt.getPageSize());

        return makeForward(mapping, request, pt);
    }

    /**
     * Hide a column.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @deprecated as we don't hide columns any more
     */
    @Deprecated public ActionForward hideColumn(ActionMapping mapping,
                                    @SuppressWarnings("unused") ActionForm form,
                                    HttpServletRequest request,
                                    @SuppressWarnings("unused") HttpServletResponse response) {
        PagedTable pt = getPagedTable(request);

        int index = Integer.parseInt(request.getParameter("index"));
        pt.getColumns().get(index).setVisible(false);

        return makeForward(mapping, request, pt);
    }

    /**
     * Show a column
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @deprecated as we don't hide columns any more
     */
    @Deprecated public ActionForward showColumn(ActionMapping mapping,
                                    @SuppressWarnings("unused") ActionForm form,
                                    HttpServletRequest request,
                                    @SuppressWarnings("unused") HttpServletResponse response) {
        PagedTable pt = getPagedTable(request);

        int index = Integer.parseInt(request.getParameter("index"));
        pt.getColumns().get(index).setVisible(true);

        return makeForward(mapping, request, pt);
    }

    /**
     * Move a column left
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @deprecated as we don't move columns any more
     */
    @Deprecated public ActionForward moveColumnLeft(ActionMapping mapping,
                                        @SuppressWarnings("unused") ActionForm form,
                                        HttpServletRequest request,
                                        @SuppressWarnings("unused") HttpServletResponse response) {
        PagedTable pt = getPagedTable(request);

        int index = Integer.parseInt(request.getParameter("index"));
        pt.moveColumnLeft(index);

        return makeForward(mapping, request, pt);
    }

    /**
     * Move a column right
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @deprecated as we don't move columns any more
     */
    @Deprecated public ActionForward moveColumnRight(ActionMapping mapping,
                                         @SuppressWarnings("unused") ActionForm form,
                                         HttpServletRequest request,
                                         @SuppressWarnings("unused") HttpServletResponse response) {
        PagedTable pt = getPagedTable(request);
        int index = Integer.parseInt(request.getParameter("index"));
        pt.moveColumnRight(index);
        return makeForward(mapping, request, pt);
    }

    /**
     * Swap two columns
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @deprecated as we don't move columns any more
     */
    @Deprecated public ActionForward swapColumns(ActionMapping mapping,
                                     @SuppressWarnings("unused") ActionForm form,
                                     HttpServletRequest request,
                                     @SuppressWarnings("unused") HttpServletResponse response) {
        PagedTable pt = getPagedTable(request);
        int index1 = Integer.parseInt(request.getParameter("index1"));
        int index2 = Integer.parseInt(request.getParameter("index2"));
        pt.swapColumns(index1, index2);
        return makeForward(mapping, request, pt);
    }

    private PagedTable getPagedTable(HttpServletRequest request) {
        String forwardName = request.getParameter("currentPage");
        PagedTable pt = null;
        if ("results".equals(forwardName)) {
            pt = SessionMethods.getResultsTable(request.getSession(), request
                            .getParameter("table"));
        } else if ("bagDetails".equals(forwardName)) {
            pt = SessionMethods.getResultsTable(request.getSession(),
                                "bag." + request.getParameter("bagName"));
        }
        return pt;
    }

    /**
     * Create a forward with parameters setting start item and page size.
     * @param mapping TODO
     * @param request the current HttpServletRequest
     * @param pt PagedTable
     *
     * @return an ActionForward with parameters
     */
    protected ActionForward makeForward(ActionMapping mapping, HttpServletRequest request,
            PagedTable pt) {
        String forwardName = request.getParameter("currentPage");
        if ("results".equals(forwardName)) {
            ForwardParameters forward = new ForwardParameters(mapping.findForward(forwardName))
                .addParameter("table", request.getParameter("table")).addParameter(
                        "page", "" + pt.getPage());
            if (request.getParameter("trail") != null) {
                forward.addParameter("trail", request.getParameter("trail"));
            }
            return forward.forward();
        } else {
            if ("bagDetails".equals(forwardName)) {
                ForwardParameters forward = new ForwardParameters(mapping.findForward(forwardName))
                    .addParameter("bagName", request.getParameter("bagName"))
                    .addParameter("page", "" + pt.getPage());
                return forward.forward();
            }
            return null;
        }
    }
}
