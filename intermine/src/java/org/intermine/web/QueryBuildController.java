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
        // these include constraint names, valid ops and suitable values for each type of
        // constraint, queryclass aliases and the cld (metadata for the active queryclass)
        HttpSession session = request.getSession();
        QueryBuildForm qbf = (QueryBuildForm) form;

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
            Map aliases = null;
            Query q = (Query) session.getAttribute("query");
            if (q != null) {
                populateForm(qbf, ConstraintHelper.createList(q, qc), q.getAliases(),
                             savedBagsInverse);
                if (constraints == null) {
                    constraints = mapConstraints(qbf.getFieldValues().keySet());
                }
                aliases = getAliases(cld, q);
                request.setAttribute("aliasStr", q.getAliases().get(qc));
            }
            
            session.setAttribute("constraints", constraints != null ? constraints : new HashMap());
            request.setAttribute("aliases", aliases != null ? aliases : new HashMap());
            request.setAttribute("ops", getOps(cld, savedBagsInverse != null
                                               && savedBagsInverse.size() > 0));
            request.setAttribute("cld", cld);
        }

        return null;
    }

    /**
     * Take a collection of constraintNames and return a Map from constraintName to fieldName
     * @param keys a collection of constraintNames
     * @return the Map
     */
    protected static Map mapConstraints(Collection keys) {
        Map constraints = new HashMap();
        for (Iterator i = keys.iterator(); i.hasNext();) {
            String constraintName = (String) i.next();
            String fieldName = constraintName.substring(0, constraintName.lastIndexOf("_"));
            constraints.put(constraintName, fieldName);
        }
        return constraints;
    }

    /**
     * Iterate through a list of Constraints, inserting the op and value of each into the form
     * keyed by constraintName
     * @param form the ActionForm used in building the query
     * @param constraints a List of Contraints on this QueryClass
     * @param aliasMap Map of QueryClass to alias in the query
     * @param savedBagsInverse an identity Map from bag to bag name
     */
    protected static void populateForm(QueryBuildForm form, List constraints, Map aliasMap,
                                      Map savedBagsInverse) {
        //map from fieldName to number of constraints on that field so far
        Map fieldNums = new HashMap();

        for (Iterator iter = constraints.iterator(); iter.hasNext();) {
            Constraint c = (Constraint) iter.next();
            String fieldName = null;
            String fieldValue = null;
            if (c instanceof SimpleConstraint) {
                SimpleConstraint sc = (SimpleConstraint) c;
                if ((sc.getArg1() instanceof QueryField)  && (sc.getArg2() instanceof QueryValue)) {
                    fieldName = ((QueryField) sc.getArg1()).getFieldName();
                    fieldValue = ((QueryValue) sc.getArg2()).getValue().toString();
                }
             } else if (c instanceof ContainsConstraint) {
                 ContainsConstraint cc = (ContainsConstraint) c;
                 fieldName = cc.getReference().getFieldName();
                 fieldValue = (String) aliasMap.get(cc.getQueryClass());
             } else if (c instanceof BagConstraint) {
                 BagConstraint bc = (BagConstraint) c;
                 if (bc.getQueryNode() instanceof QueryField) {
                     fieldName = ((QueryField) bc.getQueryNode()).getFieldName();
                     fieldValue = (String) savedBagsInverse.get(bc.getBag());
                 }
            }

            if (fieldName != null) {
                Integer num = (Integer) fieldNums.get(fieldName);
                if (num == null) {
                    num = new Integer(0);
                } else {
                    num = new Integer(num.intValue() + 1);
                }
                fieldNums.put(fieldName, num);
                String constraintName = fieldName + "_" + num;
                form.setFieldOp(constraintName, c.getOp().getIndex());
                form.setFieldValue(constraintName, fieldValue);
            }
        }
    }

    /**
     * Iterate through each reference field of a class, building up a map from field name to a list
     * of class aliases in the query that could be part of a contains constraint on that field
     * @param cld metadata for the active QueryClass
     * @param q the active Query
     * @return the revelant Map
     */
    protected static Map getAliases(ClassDescriptor cld, Query q) {
        Map values = new HashMap();

        for (Iterator iter = cld.getAllFieldDescriptors().iterator(); iter.hasNext();) {
            FieldDescriptor fd = (FieldDescriptor) iter.next();
            if (fd.isReference() || fd.isCollection()) {
                List aliases = (List) values.get(fd.getName());
                if (aliases == null) {
                    aliases = new ArrayList();
                    values.put(fd.getName(), aliases);
                }
                Class type = ((ReferenceDescriptor) fd).getReferencedClassDescriptor().getType();
                for (Iterator i = q.getFrom().iterator(); i.hasNext();) {
                    FromElement e = (FromElement) i.next();
                    if (e instanceof QueryClass && ((QueryClass) e).getType().equals(type)) {
                        aliases.add(q.getAliases().get((QueryClass) e));
                    }
                }
            }
        }

        return values;
    }

    /**
     * This method returns a map from field names to a map of operation codes to operation strings
     * For example 'name' -> 0 -> 'EQUALS'
     * @param cld the ClassDescriptor to be inspected
     * @param bagsPresent true if there are bags in the session, meaning extra valid ConstraintOps
     * @return the map
     */
    protected static Map getOps(ClassDescriptor cld, boolean bagsPresent) {
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
    protected static Map mapOps(Collection ops) {
        Map opString = new LinkedHashMap();
        for (Iterator iter = ops.iterator(); iter.hasNext();) {
            ConstraintOp op = (ConstraintOp) iter.next();
            opString.put(op.getIndex(), op.toString());
        }
        return opString;
    } 
}
