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
import java.util.HashMap;
import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;
import java.util.Iterator;

import org.apache.struts.actions.LookupDispatchAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.flymine.util.TypeUtil;
import org.flymine.metadata.Model;
import org.flymine.metadata.FieldDescriptor;
import org.flymine.metadata.AttributeDescriptor;
import org.flymine.metadata.presentation.DisplayModel;
import org.flymine.objectstore.query.*;

import org.flymine.objectstore.query.QueryValue;

import org.flymine.web.results.ResultsViewController;

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
    public ActionForward add(ActionMapping mapping,
                             ActionForm form,
                             HttpServletRequest request,
                             HttpServletResponse response)
        throws Exception {

        HttpSession session = request.getSession();

        QueryClass qc = (QueryClass) session.getAttribute("queryClass");
        if (qc != null) {
            Query q = (Query) session.getAttribute("query");
            if (q == null) {
                q = new Query();
            }
            Model model = ((DisplayModel) session.getAttribute("model")).getModel();
            if (model == null) {
                throw new Exception("Model not found in session");
            }
            QueryBuildForm queryBuildForm = (QueryBuildForm) form;
            Map newFieldValues;

            QueryHelper.removeFromQuery(q, qc);
            
            QueryHelper.addQueryClass(q, qc);
            
            Map errors =
                addToQuery(q, qc, queryBuildForm, model,
                           (Map) session.getAttribute(ResultsViewController.SAVEDBAGS_NAME),
                           (Map) session.getAttribute(SaveQueryController.SAVEDQUERIES_NAME));
            
            if (errors == null) {
                session.setAttribute("query", q);
                session.removeAttribute("queryClass");
                session.removeAttribute("constraints");
            } else {
                request.setAttribute("constraintErrors", errors);
            }
        }

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
    public ActionForward remove(ActionMapping mapping,
                                ActionForm form,
                                HttpServletRequest request,
                                HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();

        QueryClass qc = (QueryClass) session.getAttribute("queryClass");
        if (qc != null) {
            Query q = (Query) session.getAttribute("query");
            if (q != null) {
                QueryHelper.removeFromQuery(q, qc);

                if (q.getFrom().size() > 0) {
                    session.setAttribute("query", q);
                } else {
                    session.removeAttribute("query");
                }

                session.removeAttribute("queryClass");
            }
        }

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
    public ActionForward addConstraint(ActionMapping mapping,
                                       ActionForm form,
                                       HttpServletRequest request,
                                       HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();

        String fieldName = ((QueryBuildForm) form).getNewFieldName();
        Map constraints = (Map) session.getAttribute("constraints");

        // find the next valid constraintName for this fieldName
        int maxNum = -1;
        for (Iterator i = new TreeSet(constraints.keySet()).iterator(); i.hasNext();) {
            String constraint = (String) i.next();
            if (fieldName.equals(constraints.get(constraint))) {
                maxNum = Integer.parseInt(constraint.substring(constraint.lastIndexOf("_") + 1));
            }
        }

        constraints.put(fieldName + "_" + (maxNum + 1), fieldName);
        session.setAttribute("constraints", constraints);

        return mapping.findForward("buildquery");
    }

    /**
     * Examine a QueryBuildForm and add the appropriate QueryClass and
     * constraints to the current Query.  When a saved query name appears in
     * the form the corresponding Query from savedQueries is substituted into
     * the Query as a SubqueryConstraint.  When a saved collection/bag name
     * appears in the form the corresponding Collection from savedBags is
     * substituted into the Query as a BagConstraint.
     *
     * @param q a query to add QueryClass and constraints to
     * @param qc QueryClass to add to query
     * @param queryBuildForm the QueryBuildForm to extract the constraint values from
     * @param model the business model
     * @param savedBags map of saved bag names to Collections
     * @param savedQueries map of saved query names to queries
     */
    private Map addToQuery(Query q, QueryClass qc, QueryBuildForm queryBuildForm,
                            Model model, Map savedBags, Map savedQueries) {
        Map errors = null;

        Map fieldDescriptors = model.getFieldDescriptorsForClass(qc.getType());
        Map fieldValues = queryBuildForm.getFieldValues();
        Map fieldOps = queryBuildForm.getFieldOps();

        for (Iterator i = fieldValues.keySet().iterator(); i.hasNext();) {
            String fieldName = (String) i.next();
            Integer opCode = Integer.valueOf((String) fieldOps.get(fieldName));
            ConstraintOp op = ConstraintOp.getOpForIndex(opCode);
            String realFieldName = fieldName.substring(0, fieldName.lastIndexOf("_"));
            FieldDescriptor fd = (FieldDescriptor) fieldDescriptors.get(realFieldName);
            String fieldValue = (String) fieldValues.get(fieldName);

            if (fd.isAttribute()) {
                try {
                    if (BagConstraint.VALID_OPS.contains(op)) {
                        if (savedBags.containsKey(fieldValue)) {
                            Collection bag = (Collection) savedBags.get(fieldValue);
                            QueryHelper.addConstraint(q, realFieldName, qc, op, bag);
                        } else {
                            if (savedQueries.containsKey(fieldValue)) {
                                Query subQuery = (Query) savedQueries.get(fieldValue);
                                QueryHelper.addConstraint(q, realFieldName, qc, op, subQuery);
                            } else {
                                new Error ("FIXME");
                            }
                        }
                    } else {
                        Class fieldClass =
                            TypeUtil.instantiate(((AttributeDescriptor) fd).getType());
                        Object parsedFieldValue = TypeUtil.stringToObject(fieldClass, fieldValue);
                        QueryHelper.addConstraint(q, realFieldName, qc, op,
                                                  new QueryValue(parsedFieldValue));
                    }
                } catch (Exception e) {
                    if (errors == null) {
                        errors = new HashMap();
                    }
                    errors.put(fieldName, "Exception: " + e);
                }
            }
        }

        return errors;
    }

    /**
     * Distributes the actions to the necessary methods, by providing a Map from action to
     * the name of a method.
     *
     * @return a Map
     */
    protected Map getKeyMethodMap() {
        Map map = new HashMap();
        map.put("queryclass.add", "add");
        map.put("queryclass.remove", "remove");
        map.put("queryclass.addConstraint", "addConstraint");
        return map;
    }
}

