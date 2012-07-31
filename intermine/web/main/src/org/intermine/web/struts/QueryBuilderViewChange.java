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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.OrderElement;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Action to handle links on view tile
 * @author Mark Woodbridge
 */
public class QueryBuilderViewChange extends DispatchAction
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
                                        @SuppressWarnings("unused") ActionForm form,
                                        HttpServletRequest request,
                                        @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        String path = request.getParameter("path");
        PathQuery query = SessionMethods.getQuery(session);

        if (path != null) {
            // remove from the view
            query.removeView(path);
            query.removeAllIrrelevant();
        } else {
            query.clearView();
            query.clearOrderBy();
        }

        return new ForwardParameters(mapping.findForward("query"))
            .addAnchor("showing").forward();
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
                                  @SuppressWarnings("unused") ActionForm form,
                                  HttpServletRequest request,
                                  @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        int index = Integer.parseInt(request.getParameter("index"));

        PathQuery query = SessionMethods.getQuery(session);
        List<String> view = query.getView();
        query.clearView();
        String tmp = view.get(index - 1);
        view.set(index - 1, view.get(index));
        view.set(index, tmp);
        query.addViews(view);

        return new ForwardParameters(mapping.findForward("query"))
            .addAnchor("showing").forward();
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
                                   @SuppressWarnings("unused") ActionForm form,
                                   HttpServletRequest request,
                                   @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        int index = Integer.parseInt(request.getParameter("index"));

        PathQuery query = SessionMethods.getQuery(session);
        List<String> view = query.getView();
        query.clearView();
        String tmp = view.get(index + 1);
        view.set(index + 1, view.get(index));
        view.set(index, tmp);
        query.addViews(view);

        return new ForwardParameters(mapping.findForward("query"))
            .addAnchor("showing").forward();
    }
}
