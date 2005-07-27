package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

import org.apache.struts.actions.DispatchAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ConstraintOp;

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
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        PathQuery pathQuery = (PathQuery) session.getAttribute(Constants.QUERY);
        String path = request.getParameter("path");

        removeNode(pathQuery, path);

        String prefix;
        if (path.indexOf(".") == -1) {
            prefix = path;
        } else {
            prefix = path.substring(0, path.lastIndexOf("."));
            path = ((Node) pathQuery.getNodes().get(prefix)).getType();
        }

        session.setAttribute("prefix", prefix);
        session.setAttribute("path", path);

        return mapping.findForward("query");
    }

    /**
     * Remove the PathNode specified by the given (constraint) path, and it's children, from the
     * PathQuery.  Also remove any view nodes would be illegal because they depend on a type
     * constraint that will be removed.
     * @param pathQuery the PathQuery
     * @param path the path of the PathNode that should be removed.
     */
    protected static void removeNode(PathQuery pathQuery, String path) {
        // copy because we will be remove paths from the Map as we go
        Set keys = new HashSet(pathQuery.getNodes().keySet());

        // remove the node and it's children
        for (Iterator i = keys.iterator(); i.hasNext();) {
            String testPath = (String) i.next();


            if (testPath.startsWith(path)) {
                removeOneNode(pathQuery, testPath);
            }
        }
    }

    /**
     * Remove the PathNode specified by the given (constraint) path, but not it's children from the
     * PathQuery.  Also remove any view nodes would be illegal because they depend on a type
     * constraint that will be removed.
     * @param pathQuery the PathQuery
     * @param path the path of the PathNode that should be removed.
     */
    protected static void removeOneNode(PathQuery pathQuery, String path) {
        // ensure removal of any view nodes that depend on a type constraint
        // eg. Department.employees.salary where salary is only defined in a subclass of Employee
        // note that we first have to find out what type Department thinks the employees field is
        // and then check if any of the view nodes assume the field is constrained to a subclass
        String parentType = ((PathNode) pathQuery.getNodes().get(path)).getParentType();

        Model model = pathQuery.getModel();

        if (parentType != null) {
            ClassDescriptor parentCld = MainHelper.getClassDescriptor(parentType, model);
            String pathLastField = path.substring(path.lastIndexOf(".") + 1);
            FieldDescriptor fd = parentCld.getFieldDescriptorByName(pathLastField);

            if (fd instanceof ReferenceDescriptor) {
                ReferenceDescriptor rf = (ReferenceDescriptor) fd;
                ClassDescriptor realClassDescriptor = rf.getReferencedClassDescriptor();

                Iterator viewPathIter = pathQuery.getView().iterator();

                while (viewPathIter.hasNext()) {
                    String viewPath = (String) viewPathIter.next();
                    
                    if (viewPath.startsWith(path) && !viewPath.equals(path)) {
                        String fieldName = viewPath.substring(path.length() + 1);
                        
                        if (fieldName.indexOf(".") != -1) {
                            fieldName = fieldName.substring(0, fieldName.indexOf("."));
                        }

                        if (realClassDescriptor.getFieldDescriptorByName(fieldName) == null) {
                            // the field must be in a sub-class rather than the base class so remove
                            // the viewPath
                            viewPathIter.remove();
                        }
                    }
                }
            }
        }

        pathQuery.getNodes().remove(path);
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
        session.removeAttribute("editingConstraintIndex");
        session.removeAttribute("editingConstraintValue");
        session.removeAttribute("editingConstraintOperand");
        
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
     * Edit a constraint (identified by index) from a Node
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     */
    public ActionForward editConstraint(ActionMapping mapping,
                                        ActionForm form,
                                        HttpServletRequest request,
                                        HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
        String path = request.getParameter("path");
        int index = Integer.parseInt(request.getParameter("index"));

        session.setAttribute("editingNode", query.getNodes().get(path));
        session.setAttribute("editingConstraintIndex", new Integer(index));
        
        PathNode pn = (PathNode) query.getNodes().get(path);
        Constraint c = (Constraint) pn.getConstraints().get(index);
        ConstraintOp op = c.getOp();
        if (op != ConstraintOp.IS_NOT_NULL && op != ConstraintOp.IS_NULL &&
                op != ConstraintOp.CONTAINS && op != ConstraintOp.DOES_NOT_CONTAIN) {
            session.setAttribute("editingConstraintValue", c.getValue());
            session.setAttribute("editingConstraintOperand", c.getOp().getIndex());
        } else {
            session.removeAttribute("editingConstraintValue");
            session.removeAttribute("editingConstraintOperand");
        }
        
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
        
        Node node = (Node) query.getNodes().get(path);
        if (node == null) {
            node = query.addNode(path);
        }
        //automatically start editing node
        session.setAttribute("editingNode", node);
        session.removeAttribute("editingConstraintIndex");
        session.removeAttribute("editingConstraintValue");
        session.removeAttribute("editingConstraintOperand");
        //and change metadata view if relevant
        if (!node.isAttribute()) {
            session.setAttribute("prefix", path);
            session.setAttribute("path", node.getType());
        }
        
        return new ForwardParameters(mapping.findForward("query"))
                                .addAnchor("constraint-editor").forward();
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
        
        return new ForwardParameters(mapping.findForward("query")).addAnchor(path).forward();
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

        return new ForwardParameters(mapping.findForward("query")).addAnchor(path).forward();
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
