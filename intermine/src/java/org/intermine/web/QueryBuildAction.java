package org.flymine.web;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.Iterator;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;

import org.apache.struts.actions.LookupDispatchAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.flymine.metadata.Model;
import org.flymine.metadata.presentation.DisplayModel;
import org.flymine.objectstore.query.Query;

/**
 * Implementation of <strong>Action</strong> that runs a Query
 *
 * @author Richard Smith
 * @author Mark Woodbridge
 */
public class QueryBuildAction extends LookupDispatchAction
{
    /**
     * Save the Query on the and go to the FQL edit page.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward editFQL(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();

        Map queryClasses = (Map) session.getAttribute(Constants.QUERY_CLASSES);
        Map savedBags = (Map) session.getAttribute(Constants.SAVED_BAGS);
        ServletContext servletContext = session.getServletContext();
        Model model = ((DisplayModel) servletContext.getAttribute(Constants.MODEL)).getModel();

        Query q = QueryBuildHelper.createQuery(queryClasses, model, savedBags);
        session.setAttribute(Constants.QUERY, q);
        session.setAttribute(Constants.QUERY_CLASSES, null);
        session.setAttribute(Constants.EDITING_ALIAS, null);

        return mapping.findForward("buildfqlquery");
    }

    /**
     * Process the specified HTTP request, and create the corresponding HTTP
     * response (or forward to another web component that will create it).
     * Return an <code>ActionForward</code> instance describing where and how
     * control should be forwarded, or <code>null</code> if the response has
     * already been completed.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward addConstraint(ActionMapping mapping,
                                       ActionForm form,
                                       HttpServletRequest request,
                                       HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();

        Map queryClasses = (Map) session.getAttribute(Constants.QUERY_CLASSES);
        String editingAlias = (String) session.getAttribute(Constants.EDITING_ALIAS);

        if (queryClasses == null || editingAlias == null) {
            return mapping.findForward("buildquery");
        }

        QueryBuildForm qbf = (QueryBuildForm) form;

        String fieldName = qbf.getNewFieldName();
        DisplayQueryClass d = (DisplayQueryClass) queryClasses.get(editingAlias);
        if (d == null) {
            return mapping.findForward("buildquery");
        }

        List constraintNames = d.getConstraintNames();
        
        int maxNum = -1;
        for (Iterator j = new TreeSet(constraintNames).iterator(); j.hasNext();) {
            String constraintName = (String) j.next();
            if (constraintName.startsWith(fieldName)) {
                maxNum = Integer.parseInt(constraintName.substring(constraintName.lastIndexOf("_")
                                                                   + 1));
            }
        }

        String constraintName = fieldName + "_" + (maxNum + 1);
        constraintNames.add(constraintName);
        d.setFieldName(constraintName, fieldName);

        d.setFieldOps(qbf.getParsedFieldOps());
        d.setFieldValues(qbf.getParsedFieldValues());

        return mapping.findForward("buildquery");
    }

    /**
     * Process the specified HTTP request, and create the corresponding HTTP
     * response (or forward to another web component that will create it).
     * Return an <code>ActionForward</code> instance describing where and how
     * control should be forwarded, or <code>null</code> if the response has
     * already been completed.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward updateClass(ActionMapping mapping,
                                     ActionForm form,
                                     HttpServletRequest request,
                                     HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();

        Map queryClasses = (Map) session.getAttribute(Constants.QUERY_CLASSES);
        String editingAlias = (String) session.getAttribute(Constants.EDITING_ALIAS);

        QueryBuildForm qbf = (QueryBuildForm) form;

        DisplayQueryClass d = (DisplayQueryClass) queryClasses.get(editingAlias);
        d.setFieldOps(qbf.getParsedFieldOps());
        d.setFieldValues(qbf.getParsedFieldValues());
        
        session.removeAttribute(Constants.EDITING_ALIAS);

        return mapping.findForward("buildquery");
    }

    /**
     * Process the specified HTTP request, and create the corresponding HTTP
     * response (or forward to another web component that will create it).
     * Return an <code>ActionForward</code> instance describing where and how
     * control should be forwarded, or <code>null</code> if the response has
     * already been completed.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward runQuery(ActionMapping mapping,
                                  ActionForm form,
                                  HttpServletRequest request,
                                  HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();

        Map queryClasses = (Map) session.getAttribute(Constants.QUERY_CLASSES);
        Map savedBags = (Map) session.getAttribute(Constants.SAVED_BAGS);
        ServletContext servletContext = session.getServletContext();
        Model model = ((DisplayModel) servletContext.getAttribute(Constants.MODEL)).getModel();

        if (queryClasses.size() == 0) {
            throw new Exception("There are no classes present in the query");
        }
        
        //if we're editing something then update it before running query
        if (session.getAttribute(Constants.EDITING_ALIAS) != null) {
            updateClass(mapping, form, request, response);
        }

        session.setAttribute(Constants.QUERY, QueryBuildHelper.createQuery(queryClasses, model,
                                                                           savedBags));

        return mapping.findForward("runquery");
    }

    /**
     * Distributes the actions to the necessary methods, by providing a Map from action to
     * the name of a method.
     *
     * @return a Map
     */
    protected Map getKeyMethodMap() {
        Map map = new HashMap();
        map.put("button.add", "addConstraint");
        map.put("button.update", "updateClass");
        map.put("query.editfql", "editFQL");
        map.put("query.run", "runQuery");
        return map;
    }
}
