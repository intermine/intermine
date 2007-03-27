package org.intermine.web;

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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.intermine.objectstore.query.ConstraintOp;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.path.Path;
import org.intermine.web.config.FieldConfig;
import org.intermine.web.config.FieldConfigHelper;
import org.intermine.web.config.WebConfig;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;

/**
 * Action to handle links on main query builder tile.
 * 
 * @author Mark Woodbridge
 * @author Thomas Riley
 */
public class MainChange extends DispatchAction
{
    /**
     * Remove all nodes under a given path
     * 
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward removeNode(ActionMapping mapping, ActionForm form,
                                    HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        PathQuery pathQuery = (PathQuery) session.getAttribute(Constants.QUERY);
        String path = request.getParameter("path");

        removeNode(pathQuery, path);
        pathQuery.syncLogicExpression(SessionMethods.getDefaultOperator(session));

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
     * PathQuery. Also remove any view nodes would be illegal because they depend on a type
     * constraint that will be removed.
     * 
     * @param pathQuery
     *            the PathQuery
     * @param path
     *            the path of the PathNode that should be removed.
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
     * PathQuery. Also remove any view nodes would be illegal because they depend on a type
     * constraint that will be removed.
     * 
     * @param pathQuery
     *            the PathQuery
     * @param path
     *            the path of the PathNode that should be removed.
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
     * 
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward addConstraint(ActionMapping mapping, ActionForm form,
                                       HttpServletRequest request, HttpServletResponse response)
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
     * 
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward removeConstraint(ActionMapping mapping, ActionForm form,
                                          HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
        String path = request.getParameter("path");
        int index = Integer.parseInt(request.getParameter("index"));

        ((PathNode) query.getNodes().get(path)).getConstraints().remove(index);
        query.syncLogicExpression(SessionMethods.getDefaultOperator(session));
        
        return mapping.findForward("query");
    }

    /**
     * Edit a constraint (identified by index) from a Node
     * 
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward editConstraint(ActionMapping mapping, ActionForm form,
                                        HttpServletRequest request, HttpServletResponse response)
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

        if (op != ConstraintOp.IS_NOT_NULL && op != ConstraintOp.IS_NULL
            && op != ConstraintOp.CONTAINS && op != ConstraintOp.DOES_NOT_CONTAIN) {
            session.setAttribute("editingConstraintValue", c.getDisplayValue());
            session.setAttribute("editingConstraintOperand", c.getOp().getIndex());
        } else {
            session.removeAttribute("editingConstraintValue");
            session.removeAttribute("editingConstraintOperand");
        }

        return mapping.findForward("query");
    }

    /**
     * Edit a constraint's template settings (identified by index) from a Node
     * 
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward editTemplateConstraint(ActionMapping mapping, ActionForm form,
                                                HttpServletRequest request,
                                                HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
        String path = request.getParameter("path");
        int index = Integer.parseInt(request.getParameter("index"));

        session.setAttribute("editingNode", query.getNodes().get(path));
        session.setAttribute("editingConstraintIndex", new Integer(index));
        session.setAttribute("editingTemplateConstraint", Boolean.TRUE);

        return mapping.findForward("query");
    }

    /**
     * Add a Node to the query
     * 
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward addPath(ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
        String prefix = (String) session.getAttribute("prefix");
        String path = request.getParameter("path");

        path = MainHelper.toPath(prefix, path);

        // Figure out which path to delete if user cancels operation
        String bits[] = StringUtils.split(path, '.');
        String partialPath = bits[0], deletePath = "";
        for (int i = 1; i < bits.length; i++) {
            partialPath += "." + bits[i];
            if (query.getNodes().get(partialPath) == null) {
                deletePath = partialPath;
                break;
            }
        }

        Node node = (Node) query.getNodes().get(path);
        if (node == null) {
            node = query.addNode(path);
        }
        // automatically start editing node
        session.setAttribute("editingNode", node);
        session.removeAttribute("editingConstraintIndex");
        session.removeAttribute("editingConstraintValue");
        session.removeAttribute("editingConstraintOperand");

        request.setAttribute("deletePath", deletePath); // for ajax

        return new ForwardParameters(mapping.findForward("query")).addParameter("deletePath",
                                                                                deletePath)
            .addAnchor("constraint-editor").forward();
    }

    /**
     * Change the currently active metadata Node
     * 
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward changePath(ActionMapping mapping, ActionForm form,
                                    HttpServletRequest request, HttpServletResponse response)
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
     * Put query builder in template building mode.
     * 
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward startTemplateBuild(ActionMapping mapping, ActionForm form,
                                            HttpServletRequest request,
                                            HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        session.setAttribute(Constants.TEMPLATE_BUILD_STATE, new TemplateBuildState());
        return mapping.findForward("query");
    }

    /**
     * Being the query builder out of template building mode and discard any unfinished template
     * building.
     * 
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward stopTemplateBuild(ActionMapping mapping, ActionForm form,
                                           HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        session.removeAttribute(Constants.TEMPLATE_BUILD_STATE);
        session.removeAttribute(Constants.EDITING_VIEW);
        return mapping.findForward("query");
    }

    /**
     * Add a Node to the results view
     * 
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward addToView(ActionMapping mapping, ActionForm form,
                                   HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Model model = os.getModel();
        WebConfig webConfig = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);
        List view = SessionMethods.getEditingView(session);
        String pathName = request.getParameter("path");
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
        String prefix = (String) session.getAttribute("prefix");
        String fullPathName = MainHelper.toPath(prefix, pathName);

        Path path = MainHelper.makePath(model, query, fullPathName);
        // If an object has been selected, select its fields instead
        if (path.getEndFieldDescriptor() == null || path.endIsReference()
            || path.endIsCollection()) {
            ClassDescriptor cld = path.getEndClassDescriptor();
            List cldFieldConfigs = FieldConfigHelper.getClassFieldConfigs(webConfig, cld);
            Iterator cldFieldConfigIter = cldFieldConfigs.iterator();
            while (cldFieldConfigIter.hasNext()) {
                FieldConfig fc = (FieldConfig) cldFieldConfigIter.next();
                Path pathToAdd = new Path(model, path.toString() + "." + fc.getFieldExpr());
                String fullPathNoConstraints = pathToAdd.toStringNoConstraints();
                if (pathToAdd.getEndClassDescriptor() == null
                    && !view.contains(fullPathNoConstraints)) {
                    view.add(fullPathNoConstraints);
                }
            }
        } else {
            view.add(fullPathName);
        }

        ForwardParameters fp = new ForwardParameters(mapping.findForward("query"));
        fp.addAnchor(pathName);
        return new ForwardParameters(mapping.findForward("query")).addAnchor(pathName).forward();
    }

    /**
     * Select a different view to edit.
     * 
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward selectView(ActionMapping mapping, ActionForm form,
                                    HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        String name = request.getParameter("name");

        session.setAttribute(Constants.EDITING_VIEW, name);

        return new ForwardParameters(mapping.findForward("query")).forward();
    }

    /**
     * Select the default view to edit.
     * 
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward selectDefaultView(ActionMapping mapping, ActionForm form,
                                           HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        session.removeAttribute(Constants.EDITING_VIEW);

        return new ForwardParameters(mapping.findForward("query")).forward();
    }

    /**
     * Select the default view to edit.
     * 
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward renameView(ActionMapping mapping, ActionForm form,
                                    HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
        String oldName = request.getParameter("oldName");
        String newName = request.getParameter("newName");

        List view = query.getAlternativeView(oldName);
        query.removeAlternativeView(oldName);
        query.addAlternativeView(newName, view);
        session.setAttribute(Constants.EDITING_VIEW, newName);

        return new ForwardParameters(mapping.findForward("query")).forward();
    }

    /**
     * Delete an alternative view.
     * 
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward deleteView(ActionMapping mapping, ActionForm form,
                                    HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
        String name = request.getParameter("name");

        query.removeAlternativeView(name);
        session.removeAttribute(Constants.EDITING_VIEW);

        return new ForwardParameters(mapping.findForward("query")).forward();
    }

    /**
     * Delete an alternative view.
     * 
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward newView(ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
        String name = request.getParameter("name");

        query.addAlternativeView(name, new ArrayList());
        session.setAttribute(Constants.EDITING_VIEW, name);

        return new ForwardParameters(mapping.findForward("query")).forward();
    }

    /**
     * AJAX request - expand
     * 
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward ajaxExpand(ActionMapping mapping, ActionForm form,
                                    HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        changePath(mapping, form, request, response);
        MainController.populateRequest(request, response);

        // Please improve me - only build relevant Nodes in first place

        List newNodes = new ArrayList();
        Collection nodes = (Collection) request.getAttribute("nodes");
        for (Iterator iter = nodes.iterator(); iter.hasNext();) {
            Node node = (Node) iter.next();
            if (node.getPath().startsWith(request.getParameter("path") + ".")) {
                newNodes.add(node);
            }
        }
        request.setAttribute("nodes", newNodes);
        request.setAttribute("noTreeIds", Boolean.TRUE);
        return mapping.findForward("browserLines");
    }

    /**
     * AJAX request - collapse
     * 
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward ajaxCollapse(ActionMapping mapping, ActionForm form,
                                      HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        changePath(mapping, form, request, response);
        return null;
    }

    /**
     * AJAX request - show constraint panel
     * 
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward ajaxNewConstraint(ActionMapping mapping, ActionForm form,
                                           HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        addPath(mapping, form, request, response);
        return mapping.findForward("mainConstraint");
    }

    /**
     * AJAX request - edit an existing constraint
     * 
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward ajaxEditConstraint(ActionMapping mapping, ActionForm form,
                                            HttpServletRequest request,
                                            HttpServletResponse response)
        throws Exception {
        editConstraint(mapping, form, request, response);
        return mapping.findForward("mainConstraint");
    }

    /**
     * AJAX request - render query paths
     * 
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward ajaxRenderPaths(ActionMapping mapping, ActionForm form,
                                         HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        MainController.populateRequest(request, response);
        return mapping.findForward("queryPaths");
    }

}
