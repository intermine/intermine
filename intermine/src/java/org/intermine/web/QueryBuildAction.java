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

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.Iterator;

import org.apache.struts.actions.LookupDispatchAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.flymine.metadata.Model;
import org.flymine.metadata.FieldDescriptor;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.AttributeDescriptor;
import org.flymine.metadata.presentation.DisplayModel;
import org.flymine.objectstore.query.*;

/**
 * Implementation of <strong>Action</strong> that runs a Query
 *
 * @author Richard Smith
 * @author Mark Woodbridge
 */
public class QueryBuildAction extends LookupDispatchAction
{
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

        Map queryClasses = (Map) session.getAttribute("queryClasses");
        String editingAlias = (String) session.getAttribute("editingAlias");

        QueryBuildForm qbf = (QueryBuildForm) form;

        String fieldName = qbf.getNewFieldName();
        DisplayQueryClass d = (DisplayQueryClass) queryClasses.get(editingAlias);
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

        d.setFieldOps(qbf.getFieldOps());
        d.setFieldValues(qbf.getFieldValues());

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

        Map queryClasses = (Map) session.getAttribute("queryClasses");
        String editingAlias = (String) session.getAttribute("editingAlias");

        QueryBuildForm qbf = (QueryBuildForm) form;

        DisplayQueryClass d = (DisplayQueryClass) queryClasses.get(editingAlias);
        d.setFieldOps(qbf.getParsedFieldOps());
        d.setFieldValues(qbf.getParsedFieldValues());
        
        session.removeAttribute("editingAlias");

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

        Map queryClasses = (Map) session.getAttribute("queryClasses");
        Map savedBags = (Map) session.getAttribute("savedBags");
        Model model = ((DisplayModel) session.getAttribute("model")).getModel();

        if (queryClasses.size() == 0) {
            throw new Exception("There are no classes present in the query");
        }
        
        //if we're editing something then update it before running query
        if (session.getAttribute("editingAlias") != null) {
            updateClass(mapping, form, request, response);
        }

        session.setAttribute("query", createQuery(queryClasses, model, savedBags));

        return mapping.findForward("runquery");
    }
    
    /**
     * Create a Query from a Collection of DisplayQueryClasses
     * @param queryClasses the DisplayQueryClasses
     * @param model the relevant metadata
     * @param savedBags the savedBags on the session
     * @return the Query
     * @throws Exception if an error occurs in constructing the Query
     */
    protected Query createQuery(Map queryClasses, Model model, Map savedBags)
        throws Exception {
        Query q = new Query();
        Map mapping = new HashMap();
        for (Iterator i = queryClasses.keySet().iterator(); i.hasNext();) {
            String alias = (String) i.next();
            DisplayQueryClass d = (DisplayQueryClass) queryClasses.get(alias);
            QueryClass qc = new QueryClass(Class.forName(d.getType()));
            q.alias(qc, alias);
            q.addFrom(qc);
            q.addToSelect(qc);
            mapping.put(d, qc);
        }
        
        for (Iterator i = queryClasses.keySet().iterator(); i.hasNext();) {
            String alias = (String) i.next();
            DisplayQueryClass d = (DisplayQueryClass) queryClasses.get(alias);
            QueryClass qc = (QueryClass) mapping.get(d);
            ClassDescriptor cld = model.getClassDescriptorByName(d.getType());
            for (Iterator j = d.getConstraintNames().iterator(); j.hasNext();) {
                String constraintName = (String) j.next();
                String fieldName = (String) d.getFieldName(constraintName);
                FieldDescriptor fd = (FieldDescriptor) cld.getFieldDescriptorByName(fieldName);
                if (fd instanceof AttributeDescriptor) {
                    QueryHelper.addConstraint(q, fieldName, qc,
                                              (ConstraintOp) d.getFieldOp(constraintName),
                                              new QueryValue(d.getFieldValue(constraintName)));
                }
            }
        }
        return q;
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
        map.put("query.run", "runQuery");
        return map;
    }
}

