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

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.tiles.actions.TilesAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.AttributeDescriptor;
import org.flymine.metadata.Model;
import org.flymine.metadata.presentation.DisplayModel;
import org.flymine.metadata.ReferenceDescriptor;
import org.flymine.metadata.CollectionDescriptor;

import org.flymine.util.TypeUtil;
import org.flymine.objectstore.query.*;

import org.flymine.objectstore.query.ConstraintHelper;

/**
 * Perform initialisation steps for query editing tile prior to calling
 * query.jsp.
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public class QueryBuildController extends TilesAction
{
    /**
     * @see TilesAction#execute
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        HttpSession session = request.getSession();

        QueryClass qc = (QueryClass) session.getAttribute("queryClass");
        if (qc != null) {
            Model model = ((DisplayModel) session.getAttribute("model")).getModel();
            if (!model.hasClassDescriptor(qc.getType().getName())) {
                throw new Exception("ClassDescriptor (" + qc.getType().getName()
                                    + ") not found in model (" + model.getName() + ")");
            }
            ClassDescriptor cld = model.getClassDescriptorByName(qc.getType().getName());

            // find valid constraint operations for each field
            request.getSession().setAttribute("ops", getOps(cld));
            request.setAttribute("cld", cld);

            // if editing a QueryClass already on query need to populate form
            Query q = (Query) session.getAttribute("query");
            if (q != null) {
                request.setAttribute("aliases", getAliases(cld, q));

                String alias = (String) q.getAliases().get(qc);
                if (alias != null) {
                    populateQueryBuildForm((QueryBuildForm) form, q, qc);
                    request.setAttribute("aliasStr", alias);
                }
            }
        }
            return null;
    }

    /**
     * Editing an QueryClass already on query so need to fill in form from
     * constraints already on query.
     *
     * @param form the QueryBuildForm to populate
     * @param q the query being constructed
     * @param qc the QueryClass being edited
     * @throws Exception if anything goes wrong
     */
    protected void populateQueryBuildForm(QueryBuildForm form, Query q, QueryClass qc)
        throws Exception {

        List constraints = ConstraintHelper.createList(q, qc);

        Iterator iter = constraints.iterator();
        while (iter.hasNext()) {
            Constraint c = (Constraint) iter.next();
            if (c instanceof SimpleConstraint) {
                SimpleConstraint sc = (SimpleConstraint) c;
                if ((sc.getArg1() instanceof QueryField)  && (sc.getArg2() instanceof QueryValue)) {
                    form.setFieldValue(((QueryField) sc.getArg1()).getFieldName(),
                                       ((QueryValue) sc.getArg2()).getValue());

                    form.setFieldOp(((QueryField) sc.getArg1()).getFieldName(),
                                    sc.getType().getIndex());
                }
            }
        }
    }

    /**
     * This method returns a map from field names to a map of operation codes to operation strings
     * For example 'name' -> 0 -> 'EQUALS'
     * @param cld the ClassDescriptor to be inspected
     * @return the map
     */
    protected Map getOps(ClassDescriptor cld) {
        Map fieldOps = new HashMap();
        Iterator iter = cld.getAllAttributeDescriptors().iterator();
        while (iter.hasNext()) {
            AttributeDescriptor attr = (AttributeDescriptor) iter.next();
            Map opString = new LinkedHashMap();
            Iterator opIter = SimpleConstraint.validOps(TypeUtil.instantiate(attr.getType()))
                .iterator();
            while (opIter.hasNext()) {
                ConstraintOp op = (ConstraintOp) opIter.next();
                opString.put(op.getIndex(), op.toString());
            }
            fieldOps.put(attr.getName(), opString);
        }

        Map opString = new LinkedHashMap();
        Iterator opIter = ContainsConstraint.validOps().iterator();
        while (opIter.hasNext()) {
            ConstraintOp op = (ConstraintOp) opIter.next();
            opString.put(op.getIndex(), op.toString());
        }

        iter = cld.getReferenceDescriptors().iterator();
        while (iter.hasNext()) {
            ReferenceDescriptor ref = (ReferenceDescriptor) iter.next();
            fieldOps.put(ref.getName(), opString);
        }

        iter = cld.getCollectionDescriptors().iterator();
        while (iter.hasNext()) {
            CollectionDescriptor ref = (CollectionDescriptor) iter.next();
            fieldOps.put(ref.getName(), opString);
        }

        return fieldOps;
    }

    /**
     * This method returns a map from field names (for fields that are references) to a list of
     * aliases of relevant QueryNodes in the select list. It does this by iterating over the
     * relevant fields and then finding all the QueryNodes that of the same Java type in the
     * Query, adding their aliases to list.
     * @param cld the ClassDescriptor to be inspected
     * @param query the query so far
     * @return the map
     */
    protected Map getAliases(ClassDescriptor cld, Query query) {
        Map fieldAliases = new HashMap();
        Iterator iter = cld.getReferenceDescriptors().iterator();
        while (iter.hasNext()) {
            ReferenceDescriptor ref = (ReferenceDescriptor) iter.next();
            String referencedType = ref.getReferencedClassDescriptor().getName();
            List aliases = new ArrayList();
            aliases.add("");
            Iterator fromIter = query.getAliases().keySet().iterator();
            while (fromIter.hasNext()) {
                FromElement fromElem = (FromElement) fromIter.next();
                if (fromElem instanceof QueryNode) { // could be a Query
                    QueryNode qn = (QueryNode) fromElem;
                    if (qn.getType().getName().equals(referencedType)) {
                        aliases.add(query.getAliases().get(qn));
                    }
                }
            }
            fieldAliases.put(ref.getName(), aliases);
        }

        iter = cld.getCollectionDescriptors().iterator();
        while (iter.hasNext()) {
            CollectionDescriptor cod = (CollectionDescriptor) iter.next();
            String referencedType = cod.getReferencedClassDescriptor().getName();
            List aliases = new ArrayList();
            aliases.add("");
            Iterator fromIter = query.getAliases().keySet().iterator();
            while (fromIter.hasNext()) {
                FromElement fromElem = (FromElement) fromIter.next();
                if (fromElem instanceof QueryNode) { // could be a Query
                    QueryNode qn = (QueryNode) fromElem;
                    if (qn.getType().getName().equals(referencedType)) {
                        aliases.add(query.getAliases().get(qn));
                    }
                }
            }
            fieldAliases.put(cod.getName(), aliases);
        }

        return fieldAliases;
    }
}
