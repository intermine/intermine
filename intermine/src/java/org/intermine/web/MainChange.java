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
import java.util.Iterator;

import org.apache.struts.actions.DispatchAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessages;

import org.intermine.metadata.Model;

/**
 * Action to handle links on main tile
 * @author Mark Woodbridge
 */
public class MainChange extends DispatchAction
{
     /**
     * Remove all nodes under a given path
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     */
    public ActionForward removeNode(ActionMapping mapping,
                                    ActionForm form,
                                    HttpServletRequest request,
                                    HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        Map qNodes = (Map) session.getAttribute("qNodes");
        String path = request.getParameter("path");

        if (path.indexOf(".") != -1) {
            for (Iterator i = qNodes.keySet().iterator(); i.hasNext();) {
                if (((String) i.next()).startsWith(path)) {
                    i.remove();
                }
            }
        }

        ViewHelper.makeQuery(request);
        ActionMessages actionMessages = ViewHelper.makeEstimate(request);
        saveMessages(request, actionMessages);

        return mapping.findForward("query");
    }

    /**
     * Add a new constraint to this Node
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     */
    public ActionForward addConstraint(ActionMapping mapping,
                                       ActionForm form,
                                       HttpServletRequest request,
                                       HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        Map qNodes = (Map) session.getAttribute("qNodes");
        String path = request.getParameter("path");

        request.setAttribute("editingNode", qNodes.get(path));

        ViewHelper.makeQuery(request);
        ActionMessages actionMessages = ViewHelper.makeEstimate(request);
        saveMessages(request, actionMessages);

        return mapping.findForward("query");
    }

    /**
     * Remove a constraint (identified by index) from a Node
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     */
    public ActionForward removeConstraint(ActionMapping mapping,
                                          ActionForm form,
                                          HttpServletRequest request,
                                          HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        Map qNodes = (Map) session.getAttribute("qNodes");
        String path = request.getParameter("path");
        int index = Integer.parseInt(request.getParameter("index"));

        ((RightNode) qNodes.get(path)).getConstraints().remove(index);

        ViewHelper.makeQuery(request);
        ActionMessages actionMessages = ViewHelper.makeEstimate(request);
        saveMessages(request, actionMessages);

        return mapping.findForward("query");
    }

    /**
     * Add a Node to the query
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     */
    public ActionForward addPath(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Model model = (Model) servletContext.getAttribute(Constants.MODEL);
        Map qNodes = (Map) session.getAttribute("qNodes");
        String prefix = (String) session.getAttribute("prefix");
        String path = request.getParameter("path");

        if (prefix != null) {
            path = prefix + "." + path.substring(path.indexOf(".") + 1);
        }
        MainHelper.addNode(qNodes, path, model);
        Node node = (Node) qNodes.get(path);
        //automatically start editing node
        request.setAttribute("editingNode", node);
        //and change metadata view if relevant
        if (!node.isAttribute()) {
            session.setAttribute("prefix", path);
            path = MainHelper.getType(path, model);
            session.setAttribute("path", path);
        }
        
        ViewHelper.makeQuery(request);
        ActionMessages actionMessages = ViewHelper.makeEstimate(request);
        saveMessages(request, actionMessages);

        return mapping.findForward("query");
    }

    /**
     * Change the currently active metadata Node
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     */
    public ActionForward changePath(ActionMapping mapping,
                                    ActionForm form,
                                    HttpServletRequest request,
                                    HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        String path = request.getParameter("path");
        String prefix = request.getParameter("prefix");

        session.setAttribute("path", path);
        if (prefix != null) {
            session.setAttribute("prefix", prefix);
        }

        ViewHelper.makeQuery(request);
        ActionMessages actionMessages = ViewHelper.makeEstimate(request);
        saveMessages(request, actionMessages);

        return mapping.findForward("query");
    }

    /**
     * Add a Node to the results view
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     */
    public ActionForward addToView(ActionMapping mapping,
                                   ActionForm form,
                                   HttpServletRequest request,
                                   HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        List view = (List) session.getAttribute("view");
        String prefix = (String) session.getAttribute("prefix");
        String path = request.getParameter("path");

        if (prefix != null) {
            path = prefix + (path.indexOf(".") == -1
                             ? ""
                             : "." + path.substring(path.indexOf(".") + 1));
        }
        view.add(path);

        ViewHelper.makeQuery(request);
        ActionMessages actionMessages = ViewHelper.makeEstimate(request);
        saveMessages(request, actionMessages);

        return mapping.findForward("query");
    }
}
