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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.pathquery.Node;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintLoop;
import org.intermine.pathquery.PathConstraintSubclass;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.TemplateQuery;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.querybuilder.DisplayPath;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Action to handle links on main query builder tile.
 *
 * @author Mark Woodbridge
 * @author Thomas Riley
 */
public class QueryBuilderChange extends DispatchAction
{
    protected static final Logger LOG = Logger.getLogger(QueryBuilderChange.class);

    /**
     * Remove everything in a query under a given path.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     */
    public ActionForward removeNode(ActionMapping mapping,
            ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();

        PathQuery pathQuery = SessionMethods.getQuery(session);
        String path = request.getParameter("path");

        pathQuery.removeAllUnder(path);

        return mapping.findForward("query");
    }

    /**
     * Add a new constraint to the query.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward newConstraint(ActionMapping mapping,
            ActionForm form,
            HttpServletRequest request,
            HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        PathQuery query = SessionMethods.getQuery(session).clone();

        String path = request.getParameter("path");

        session.setAttribute("newConstraintPath", new DisplayPath(query.makePath(path)));

        return new ForwardParameters(mapping.findForward("query")).forward();
    }

    /**
     * Remove a constraint identified by its code from the query.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward removeConstraint(ActionMapping mapping,
            ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        PathQuery query = SessionMethods.getQuery(session);
        String code = request.getParameter("code");
        query.removeConstraint(query.getConstraintForCode(code));
        query.removeAllIrrelevant();

        return mapping.findForward("query");
    }

    /**
     * Remove a subclass constraint from the query.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form the optional ActionForm bean for this request
     * @param request the HTTP request we are processing
     * @param response the HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @throws Exception if something goes wrong
     */
    public ActionForward removeSubclass(ActionMapping mapping,
            ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        PathQuery query = SessionMethods.getQuery(session);
        String path = request.getParameter("path");
        Collection<String> msgs = query.removeSubclassAndFixUp(path);
        for (String message : msgs) {
            SessionMethods.recordMessage(message, session);
        }
        return mapping.findForward("query");
    }

    /**
     * Edit a constraint on the query identified by the constraint code.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward editConstraint(ActionMapping mapping,
                                        ActionForm form,
                                        HttpServletRequest request,
                                        HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        PathQuery query = SessionMethods.getQuery(session);
        String code = request.getParameter("code");

        PathConstraint c = query.getConstraintForCode(code);
        session.setAttribute("editingConstraint", c);

        return mapping.findForward("query");
    }

    /**
     * Edit a constraint's template settings identified by code in query.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward editTemplateConstraint(ActionMapping mapping,
                                                ActionForm form,
                                                HttpServletRequest request,
                                                HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        PathQuery query = SessionMethods.getQuery(session);
        String code = request.getParameter("code");

        session.setAttribute("editingTemplateConstraint", Boolean.TRUE);
        PathConstraint c = query.getConstraintForCode(code);
        session.setAttribute("editingConstraint", c);
        return mapping.findForward("query");
    }

    /**
     * Edit a the join style settings for a path.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward editJoinStyle(ActionMapping mapping,
                                                ActionForm form,
                                                HttpServletRequest request,
                                                HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        String path = request.getParameter("path");
        session.setAttribute("joinStylePath", path);
        return mapping.findForward("query");
    }


    /**
     * Change the currently active metadata Node
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward changePath(ActionMapping mapping,
            ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        String path = request.getParameter("path");

        session.setAttribute("path", path);

        return new ForwardParameters(mapping.findForward("query")).forward();
    }

    /**
     * Put query builder in template building mode.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward startTemplateBuild(ActionMapping mapping,
                                            ActionForm form,
                                            HttpServletRequest request,
                                            HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        PathQuery query = SessionMethods.getQuery(session);
        TemplateQuery template = new TemplateQuery("", "", "", query);
        for (PathConstraint con : query.getConstraints().keySet()) {
            if (!(con instanceof PathConstraintLoop) && !(con instanceof PathConstraintSubclass)) {
                template.setEditable(con, true);
            }
        }
        SessionMethods.loadQuery(template, session, response);
        session.setAttribute(Constants.NEW_TEMPLATE, Boolean.TRUE);
        session.removeAttribute(Constants.EDITING_TEMPLATE);
        session.removeAttribute(Constants.PREV_TEMPLATE_NAME);
        return mapping.findForward("query");
    }

    /**
     * Bring the query builder out of template building mode and discard any unfinished template
     * building.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward stopTemplateBuild(ActionMapping mapping,
                                           ActionForm form,
                                           HttpServletRequest request,
                                           HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        session.removeAttribute(Constants.NEW_TEMPLATE);
        session.removeAttribute(Constants.EDITING_TEMPLATE);
        return mapping.findForward("query");
    }

    /**
     * Add a path to the results view.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward addToView(ActionMapping mapping,
                                   ActionForm form,
                                   HttpServletRequest request,
                                   HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        WebConfig webConfig = SessionMethods.getWebConfig(request);

        String pathName = request.getParameter("path");
        PathQuery query = SessionMethods.getQuery(session);
        if (query.getView().contains(pathName)) {
            return new ForwardParameters(mapping.findForward("query")).forward();
        }
        Path path = query.makePath(pathName);

        // If an object has been selected, select its fields instead
        if (path.isRootPath() || path.endIsReference() || path.endIsCollection()) {
            ClassDescriptor cld = path.getEndClassDescriptor();
            for (FieldConfig fc : FieldConfigHelper.getClassFieldConfigs(webConfig, cld)) {
                Path pathToAdd = query.makePath(path.toStringNoConstraints()
                        + "." + fc.getFieldExpr());

                if (pathToAdd.endIsAttribute()
                        && (!query.getView().contains(pathToAdd.getNoConstraintsString()))
                        && (fc.getDisplayer() == null && fc.getShowInSummary())) {
                    query.addView(pathToAdd.getNoConstraintsString());
                }
            }
        } else {
            query.addView(pathName);
        }

        // if the sort order is empty, sort by the first view element valid for sorting (if any)
        if (query.getOrderBy().isEmpty()) {
            for (String view : query.getView()) {
                if (query.isPathCompletelyInner(view)) {
                    query.addOrderBy(view, OrderDirection.ASC);
                    break;
                }
            }
        }

        return new ForwardParameters(mapping.findForward("query")).forward();
    }


    /**
     * AJAX request - expand a model browser node.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    @SuppressWarnings("unchecked")
    public ActionForward ajaxExpand(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        changePath(mapping, form, request, response);
        QueryBuilderController.populateRequest(request, response);

        // Please improve me - only build relevant Nodes in first place

        List<Node> newNodes = new ArrayList<Node>();
        Collection<Node> nodes = (Collection<Node>) request.getAttribute("nodes");
        for (Node node : nodes) {
            if (node.getPathString().startsWith(request.getParameter("path") + ".")) {
                newNodes.add(node);
            }
        }
        request.setAttribute("nodes", newNodes);
        request.setAttribute("noTreeIds", Boolean.TRUE);
        request.setAttribute("scrollTo", request.getParameter("path"));
        return mapping.findForward("browserLines");
    }

    /**
     * AJAX request - collapse a model browser node.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward ajaxCollapse(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        changePath(mapping, form, request, response);
        return null;
    }

    /**
     * AJAX request - ahow the panel for adding a new constraint to the query.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward ajaxNewConstraint(ActionMapping mapping, ActionForm form,
                                           HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        newConstraint(mapping, form, request, response);
        return mapping.findForward("queryBuilderConstraint");
    }

    /**
     * AJAX request - edit an existing constraint.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward ajaxEditConstraint(ActionMapping mapping, ActionForm form,
                                            HttpServletRequest request,
                                            HttpServletResponse response)
        throws Exception {
        editConstraint(mapping, form, request, response);
        return mapping.findForward("queryBuilderConstraint");
    }


    /**
     * AJAX request - edit a template constraint.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward ajaxEditTemplateConstraint(ActionMapping mapping, ActionForm form,
                                            HttpServletRequest request,
                                            HttpServletResponse response)
        throws Exception {
        editTemplateConstraint(mapping, form, request, response);
        return mapping.findForward("queryBuilderConstraint");
    }

    /**
     * AJAX request - render query paths.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws
     */
    public ActionForward ajaxRenderPaths(ActionMapping mapping,
            ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        QueryBuilderController.populateRequest(request, response);
        return mapping.findForward("queryPaths");
    }

    /**
     * Edit a constraint's join style settings for a path, and forward to queryBuilderConstraint,
     * for use by ajax.
     *
     * @param mapping the action mapping
     * @param form struts form
     * @param request the request
     * @param response the response
     * @return the forward
     * @throws Exception if something goes wrong
     */
    public ActionForward ajaxEditJoinStyle(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        editJoinStyle(mapping, form, request, response);
        return mapping.findForward("queryBuilderConstraint");
    }
}
