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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;

import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.AttributeDescriptor;
import org.flymine.metadata.FieldDescriptor;
import org.flymine.metadata.Model;
import org.flymine.metadata.presentation.DisplayModel;
import org.flymine.metadata.ReferenceDescriptor;
import org.flymine.metadata.CollectionDescriptor;
import org.flymine.objectstore.query.*;
import org.flymine.util.TypeUtil;

/**
 * Perform initialisation steps for query editing tile prior to calling query.jsp
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
        // this method sets up things used by queryBuild.jsp to render the query.
        // these include constraint names, valid ops for each type of
        // constraint, queryclass aliases and the cld (metadata for the active queryclass)
        HttpSession session = request.getSession();

        QueryClass qc = (QueryClass) session.getAttribute("queryClass");
        if (qc != null) {
            Model model = ((DisplayModel) session.getAttribute("model")).getModel();
            ClassDescriptor cld = model.getClassDescriptorByName(qc.getType().getName());
            if (cld == null) {
                throw new Exception("ClassDescriptor (" + qc.getType().getName()
                                    + ") not found in model (" + model.getName() + ")");
            }

            Map savedBagsInverse = (Map) session.getAttribute("savedBagsInverse");
            Map constraints = (Map) session.getAttribute("constraints");
            Query q = (Query) session.getAttribute("query");
            if (q != null) {
                if (constraints == null) {
                    constraints = buildConstraintMap((QueryBuildForm) form,
                                                     ConstraintHelper.createList(q, qc),
                                                     q.getAliases(), savedBagsInverse);
                }
                request.setAttribute("aliases", getAliases(cld, q));
                request.setAttribute("aliasStr", q.getAliases().get(qc));
            }
            
            session.setAttribute("constraints", constraints != null ? constraints : new HashMap());
            request.setAttribute("ops", getOps(cld, savedBagsInverse != null
                                               && savedBagsInverse.size() > 0));
            request.setAttribute("cld", cld);
        }

        return null;
    }

    /**
     * Iterate through a List of Constraints, basically finding out how many constraints there are
     * for each field. This is encoded in the resultant Map.
     *
     * @param form the ActionForm used in building the query
     * @param constraints a List of Contraints on this QueryClass
     * @param aliasMap Map of QueryClass to alias in the query
     * @param savedBagsInverse an identity Map from bag to bag name
     * @return a Map from constraint name (fieldName_constraintNumber) to fieldName
     */
    protected Map buildConstraintMap(QueryBuildForm form, List constraints, Map aliasMap,
                                     Map savedBagsInverse) {
        // map from constraint name to fieldName
        Map constraintNames = new HashMap();
        //map from fieldName to number of constraints on that field so far
        Map fieldNums = new HashMap();

        for (Iterator iter = constraints.iterator(); iter.hasNext();) {
            Constraint c = (Constraint) iter.next();
            Integer fieldOp = c.getOp().getIndex();
            String fieldName = null;
            Object fieldValue = null;
            if (c instanceof SimpleConstraint) {
                SimpleConstraint sc = (SimpleConstraint) c;
                if ((sc.getArg1() instanceof QueryField)  && (sc.getArg2() instanceof QueryValue)) {
                    fieldName = ((QueryField) sc.getArg1()).getFieldName();
                    fieldValue = ((QueryValue) sc.getArg2()).getValue();
                }
             } else if (c instanceof ContainsConstraint) {
                 ContainsConstraint cc = (ContainsConstraint) c;
                 fieldName = cc.getReference().getFieldName();
                 fieldValue = aliasMap.get(cc.getQueryClass());
             } else if (c instanceof BagConstraint) {
                 BagConstraint bc = (BagConstraint) c;
                 if (bc.getQueryNode() instanceof QueryField) {
                     fieldName = ((QueryField) bc.getQueryNode()).getFieldName();
                     fieldValue = savedBagsInverse.get(bc.getBag());
                 }
            }
            if (fieldName != null) {
                Integer num = (Integer) fieldNums.get(fieldName);
                if (num == null) {
                    num = new Integer(0);
                }
                fieldNums.put(fieldName, new Integer(num.intValue() + 1));
                String constraintName = fieldName + "_" + num;
                constraintNames.put(constraintName, fieldName);
                form.setFieldOp(constraintName, fieldOp);
                form.setFieldValue(constraintName, fieldValue);
            }
        }

        return constraintNames;
    }

    /**
     * This method returns a map from field names to a map of operation codes to operation strings
     * For example 'name' -> 0 -> 'EQUALS'
     * @param cld the ClassDescriptor to be inspected
     * @param bagsPresent true if there are bags in the session, meaning extra valid ConstraintOps
     * @return the map
     */
    protected Map getOps(ClassDescriptor cld, boolean bagsPresent) {
        Map fieldOps = new HashMap();

        //attributes
        for (Iterator iter = cld.getAllAttributeDescriptors().iterator(); iter.hasNext();) {
            AttributeDescriptor attr = (AttributeDescriptor) iter.next();
            List validOps = SimpleConstraint.validOps(TypeUtil.instantiate(attr.getType()));
            Set ops = new LinkedHashSet(validOps);
            if (bagsPresent) {
                ops.addAll(BagConstraint.VALID_OPS);
            }
            fieldOps.put(attr.getName(), mapOps(ops));
        }

        //references and collections
        Map opString = mapOps(ContainsConstraint.VALID_OPS);
        for (Iterator iter = cld.getAllFieldDescriptors().iterator(); iter.hasNext();) {
            FieldDescriptor fd = (FieldDescriptor) iter.next();
            if (fd.isReference() || fd.isCollection()) {
                fieldOps.put(fd.getName(), opString);
            }
        }

        return fieldOps;
    }

    /**
     * Take a Collection of ConstraintOps and builds a map from ConstraintOp.getIndex() to
     * ConstraintOp.toString() for each
     * @param ops a Collection of ConstraintOps
     * @return the Map from index to string
     */
    protected Map mapOps(Collection ops) {
        Map opString = new LinkedHashMap();
        for (Iterator iter = ops.iterator(); iter.hasNext();) {
            ConstraintOp op = (ConstraintOp) iter.next();
            opString.put(op.getIndex(), op.toString());
        }
        return opString;
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
