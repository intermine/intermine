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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.intermine.web.logic.Constants;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.session.SessionMethods;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;

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
                                        @SuppressWarnings("unused") ActionForm form,
                                        HttpServletRequest request,
                                        @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        String path = request.getParameter("path");
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
        
        query.removePathStringFromView(path);

        // remove from sort list too, only if this is the sorted field
        query.removePathStringFromSortOrder(path);
        
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

        List view = SessionMethods.getEditingView(session);
        Object o = view.get(index - 1);
        view.set(index - 1, view.get(index));
        view.set(index, o);

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

        List view = SessionMethods.getEditingView(session);
        Object o = view.get(index + 1);
        view.set(index + 1, view.get(index));
        view.set(index, o);

        return new ForwardParameters(mapping.findForward("query"))
            .addAnchor("showing").forward();
    }
    
    /**
     * AJAX request - reorder view.
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     */
    public ActionForward reorder(@SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused")   ActionForm form,
                                   HttpServletRequest request,
                                   @SuppressWarnings("unused")   HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        String newOrder[] = request.getParameterValues("viewDivs[]");
        String oldOrder[] = request.getParameterValues("oldOrder[]");
        
        List view = SessionMethods.getEditingView(session);
        ArrayList newView = new ArrayList();
        
        for (int i = 0; i < view.size(); i++) {
            int newi = Integer.parseInt(newOrder[i]);
            int oldi = Arrays.asList(oldOrder).indexOf("" + newi);
            newView.add(view.get(oldi));
        }
        
        view.clear();
        view.addAll(newView);
        
        return null;
    }
}
