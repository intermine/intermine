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
        HttpSession session = request.getSession();

        Map queryClasses = (Map) session.getAttribute("queryClasses");
        String editingAlias = (String) session.getAttribute("editingAlias");
        Map savedBagsInverse = (Map) session.getAttribute("savedBagsInverse");
        Model model = ((DisplayModel) session.getAttribute("model")).getModel();
        Query q = (Query) session.getAttribute("query");

        if (queryClasses == null) {
            session.setAttribute("queryClasses", new LinkedHashMap());
        }
        
        //there's a query on the session but it hasn't been rendered yet
        if (q != null) {
            queryClasses = new LinkedHashMap();
            for (Iterator i = q.getFrom().iterator(); i.hasNext();) {
                FromElement fe = (FromElement) i.next();
                if (!(fe instanceof QueryClass)) {
                    continue;
                }
                QueryClass qc = (QueryClass) fe;
                
                queryClasses.put((String) q.getAliases().get(qc),
                                 toDisplayable(qc, q, savedBagsInverse));
            }

            session.setAttribute("queryClasses", queryClasses);
            session.setAttribute("query", null);
        }

        //we are editing a QueryClass - render it as a form
        if (editingAlias != null) {
            String type = ((DisplayQueryClass) queryClasses.get(editingAlias)).getType();
            ClassDescriptor cld = model.getClassDescriptorByName(type);
            
            DisplayQueryClass d = (DisplayQueryClass) queryClasses.get(editingAlias);
            QueryBuildForm qbf = (QueryBuildForm) form;
            qbf.setFieldOps(toStrings(d.getFieldOps()));
            qbf.setFieldValues(toStrings(d.getFieldValues()));
            
            session.setAttribute("validOps", getValidOps(cld, false));
            session.setAttribute("allFieldNames", getAllFieldNames(cld));
        }

        return null;
    }
    
    /**
     * Take a Map and convert all its values to Strings using toString()
     * @param input the input Map
     * @return the output Map
     */
    protected static Map toStrings(Map input) {
        Map output = new HashMap();
        for (Iterator i = input.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            output.put(key, input.get(key).toString());
        }
        return output;
    }

    /**
     * Take a ClassDescriptor and return a List of the names of all its fields
     * @param cld the ClassDescriptor
     * @return the relevant List
     */
    protected static List getAllFieldNames(ClassDescriptor cld) {
        List allFieldNames = new ArrayList();
        for (Iterator i = cld.getAllFieldDescriptors().iterator(); i.hasNext();) {
            allFieldNames.add(((FieldDescriptor) i.next()).getName());
        }
        return allFieldNames;
    }
    
    /**
     * This method returns a map from field names to a map of operation codes to operation strings
     * For example 'name' -> 0 -> 'EQUALS'
     * @param cld the ClassDescriptor to be inspected
     * @param bagsPresent true if there are bags in the session, meaning extra valid ConstraintOps
     * @return the map
     */
    protected static Map getValidOps(ClassDescriptor cld, boolean bagsPresent) {
        Map fieldOps = new HashMap();

        //attributes - allow valid ops for simpleconstraints plus IN/NOT IN if bags present
        for (Iterator iter = cld.getAllAttributeDescriptors().iterator(); iter.hasNext();) {
            AttributeDescriptor attr = (AttributeDescriptor) iter.next();
            List validOps = SimpleConstraint.validOps(TypeUtil.instantiate(attr.getType()));
            Set ops = new LinkedHashSet(validOps);
            if (bagsPresent) {
                ops.addAll(BagConstraint.VALID_OPS);
            }
            fieldOps.put(attr.getName(), mapOps(ops));
        }

        //references and collections - allow valid ops for containsconstraints
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

    /**
     * Convert a QueryClass to a DisplayableQueryClass
     * @param qc the QueryClass
     * @param q the Query the QueryClass is a part of
     * @param savedBagsInverse a map from bag to bag name for name resolution
     * @return a DisplayQueryClass
     */
    protected static DisplayQueryClass toDisplayable(QueryClass qc, Query q, Map savedBagsInverse) {
        DisplayQueryClass d = new DisplayQueryClass();
        d.setType(qc.getType().getName());

        //fieldNames and constraintNames
        Map fieldNums = new HashMap();
        
        for (Iterator i = ConstraintHelper.createList(q, qc).iterator(); i.hasNext();) {
            String fieldName = null;
            Object fieldValue = null;
            Constraint c = (Constraint) i.next();
            if (c instanceof SimpleConstraint) {
                SimpleConstraint sc = (SimpleConstraint) c;
                if ((sc.getArg1() instanceof QueryField)  && (sc.getArg2() instanceof QueryValue)) {
                    fieldName = ((QueryField) sc.getArg1()).getFieldName();
                    fieldValue = ((QueryValue) sc.getArg2()).getValue();
                }
            } else if (c instanceof ContainsConstraint) {
                ContainsConstraint cc = (ContainsConstraint) c;
                fieldName = cc.getReference().getFieldName();
                fieldValue = (String) q.getAliases().get(cc.getQueryClass());
            } else if (c instanceof BagConstraint) {
                BagConstraint bc = (BagConstraint) c;
                if (bc.getQueryNode() instanceof QueryField) {
                    fieldName = ((QueryField) bc.getQueryNode()).getFieldName();
                    fieldValue = (String) savedBagsInverse.get(bc.getBag());
                }
            }
            
            Integer num = (Integer) fieldNums.get(fieldName);
            if (num == null) {
                num = new Integer(0);
            } else {
                num = new Integer(num.intValue() + 1);
            }
            fieldNums.put(fieldName, num);
            String constraintName = fieldName + "_" + num;
            d.getConstraintNames().add(constraintName);
            d.setFieldName(constraintName, fieldName);
            d.setFieldOp(constraintName, c.getOp());
            d.setFieldValue(constraintName, fieldValue);
        }

        return d;
    }
}

