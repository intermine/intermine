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
import javax.servlet.ServletContext;

import java.util.Map;
import java.util.List;

import org.apache.struts.actions.DispatchAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.apache.struts.action.ActionMessages;
import org.intermine.metadata.Model;

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
        List view = (List) session.getAttribute("view");
        String path = request.getParameter("path");

        view.remove(path);

        ViewHelper.makeQuery(request);
        ActionMessages actionMessages = ViewHelper.makeEstimate(request);
        saveMessages(request, actionMessages);

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
        List view = (List) session.getAttribute("view");
        int index = Integer.parseInt(request.getParameter("index"));

        Object o = view.get(index - 1);
        view.set(index - 1, view.get(index));
        view.set(index, o);

        ViewHelper.makeQuery(request);
        ActionMessages actionMessages = ViewHelper.makeEstimate(request);
        saveMessages(request, actionMessages);

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
        List view = (List) session.getAttribute("view");
        int index = Integer.parseInt(request.getParameter("index"));

        Object o = view.get(index + 1);
        view.set(index + 1, view.get(index));
        view.set(index, o);

        ViewHelper.makeQuery(request);
        ActionMessages actionMessages = ViewHelper.makeEstimate(request);
        saveMessages(request, actionMessages);

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
        ServletContext servletContext = session.getServletContext();
        Model model = (Model) servletContext.getAttribute(Constants.MODEL);
        Map savedBags = (Map) session.getAttribute(Constants.SAVED_BAGS);
        Map savedQueries = (Map) session.getAttribute(Constants.SAVED_QUERIES);

        ViewHelper.makeQuery(request);

        ActionMessages actionMessages = ViewHelper.runQuery(request);
        saveMessages(request, actionMessages);

        return mapping.findForward("results");
    }

    /**
     * Run the query and forward to the export page.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward export(ActionMapping mapping,
                                ActionForm form,
                                HttpServletRequest request,
                                HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Model model = (Model) servletContext.getAttribute(Constants.MODEL);
        Map savedBags = (Map) session.getAttribute(Constants.SAVED_BAGS);
        Map savedQueries = (Map) session.getAttribute(Constants.SAVED_QUERIES);

        ViewHelper.makeQuery(request);

        ActionMessages actionMessages = ViewHelper.runQuery(request);
        saveMessages(request, actionMessages);

        return mapping.findForward("export");
    }
}
