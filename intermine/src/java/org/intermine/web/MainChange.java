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

import java.util.Iterator;

import org.apache.struts.actions.DispatchAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

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
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
        String path = request.getParameter("path");

        for (Iterator i = query.getNodes().keySet().iterator(); i.hasNext();) {
            if (((String) i.next()).startsWith(path)) {
                i.remove();
            }
        }

        String prefix;
        if (path.indexOf(".") == -1) {
            prefix = path;
        } else {
            prefix = path.substring(0, path.lastIndexOf("."));
            path = ((Node) query.getNodes().get(prefix)).getType();
        }
        session.setAttribute("prefix", prefix);
        session.setAttribute("path", path);


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
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
        String path = request.getParameter("path");

        session.setAttribute("editingNode", query.getNodes().get(path));

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
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
        String path = request.getParameter("path");
        int index = Integer.parseInt(request.getParameter("index"));

        ((PathNode) query.getNodes().get(path)).getConstraints().remove(index);

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
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
        String prefix = (String) session.getAttribute("prefix");
        String path = request.getParameter("path");

        path = toPath(prefix, path);
        Node node = query.addNode(path);
        //automatically start editing node
        session.setAttribute("editingNode", node);
        //and change metadata view if relevant
        if (!node.isAttribute()) {
            session.setAttribute("prefix", path);
            session.setAttribute("path", node.getType());
        }
        
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
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
        String prefix = (String) session.getAttribute("prefix");
        String path = request.getParameter("path");

        query.getView().add(toPath(prefix, path));

        return mapping.findForward("query");
    }

    /**
     * Convert a path and prefix to a path
     * @param prefix the prefix (eg null or Department.company)
     * @param path the path (eg Company, Company.departments)
     * @return the new path
     */
    protected static String toPath(String prefix, String path) {
        if (prefix != null) {
            if (path.indexOf(".") == -1) {
                path = prefix;
            } else {
                path = prefix + "." + path.substring(path.indexOf(".") + 1);
            }
        }
        return path;
    }
}
