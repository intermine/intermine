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
import org.flymine.metadata.ReferenceDescriptor;
import org.flymine.util.TypeUtil;
import org.flymine.objectstore.query.*;

import org.flymine.objectstore.query.presentation.ConstraintListCreator;
import org.flymine.objectstore.query.presentation.AssociatedConstraint;

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

        ClassDescriptor cld = (ClassDescriptor) session.getAttribute("cld");
        if (cld != null) {
            session.setAttribute("ops", getOps(cld));
            Query query = (Query) session.getAttribute("query");
            if (query != null) {
                session.setAttribute("aliases", getAliases(cld, query));
            }
        }

        form.reset(mapping, request);
        if (session.getAttribute("alias") != null) {
            populateQueryBuildForm((QueryBuildForm) form, session);
            request.setAttribute("aliasStr", session.getAttribute("alias"));
            session.removeAttribute("alias");
        }
        return null;
    }

    /**
     * Editing an QueryClass already on query so need to fill in form from
     * constraints already on query.
     *
     * @param form the QueryBuildForm to populate
     * @param session HttpSession to get alias and query from
     * @throws Exception if anything goes wrong
     */
    protected void populateQueryBuildForm(QueryBuildForm form, HttpSession session)
        throws Exception {

        String alias = (String) session.getAttribute("alias");
        Query q = (Query) session.getAttribute("query");
        QueryClass qc = (QueryClass) q.getReverseAliases().get(alias);
        List constraints = ConstraintListCreator.createList(q, qc);

        Iterator iter = constraints.iterator();
        while (iter.hasNext()) {
            AssociatedConstraint ac = (AssociatedConstraint) iter.next();
            Constraint c = ac.getConstraint();
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
                QueryOp op = (QueryOp) opIter.next();
                opString.put(op.getIndex(), op.toString());
            }
            fieldOps.put(attr.getName(), opString);
        }
        iter = cld.getReferenceDescriptors().iterator();
        while (iter.hasNext()) {
            ReferenceDescriptor ref = (ReferenceDescriptor) iter.next();
            Map opString = new LinkedHashMap();
            Iterator opIter = ContainsConstraint.validOps().iterator();
            while (opIter.hasNext()) {
                QueryOp op = (QueryOp) opIter.next();
                opString.put(op.getIndex(), op.toString());
            }
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
        return fieldAliases;
    }
}
