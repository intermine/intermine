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

import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.AttributeDescriptor;
import org.flymine.metadata.FieldDescriptor;
import org.flymine.metadata.ReferenceDescriptor;
import org.flymine.metadata.Model;
import org.flymine.objectstore.query.QueryReference;
import org.flymine.objectstore.query.QueryObjectReference;
import org.flymine.objectstore.query.QueryCollectionReference;
import org.flymine.objectstore.query.SimpleConstraint;
import org.flymine.objectstore.query.BagConstraint;
import org.flymine.objectstore.query.ContainsConstraint;
import org.flymine.objectstore.query.ConstraintHelper;
import org.flymine.objectstore.query.Constraint;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryField;
import org.flymine.objectstore.query.FromElement;
import org.flymine.objectstore.query.ConstraintOp;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryHelper;
import org.flymine.objectstore.query.QueryValue;
import org.flymine.util.TypeUtil;

/**
 * Helper methods for Query building (QueryBuildController and QueryBuildAction)
 *
 * @author Kim Rutherford
 * @author Mark Woodbridge
 */
public class QueryBuildHelper
{
    /**
     * Return the field name (in an object) for a given field name in the form.
     *
     * @param fieldName the field name from a form.
     * @return the field name as it appears in the class.
     */
    public static String getFieldName(String fieldName) {
        return fieldName.substring(0, fieldName.lastIndexOf("_"));
    }

    /**
     * Give a class name a unique alias given the existing aliases (eg Company -> Company_3)
     * @param existingAliases the Collection of existing aliases
     * @param type the class name
     * @return a new alias
     */
    protected static String aliasClass(Collection existingAliases, String type) {
        String prefix = toAlias(type);
        int max = 0;
        for (Iterator i = existingAliases.iterator(); i.hasNext();) {
            String alias = (String) i.next();
            if (alias.substring(0, alias.lastIndexOf("_")).equals(prefix)) {
                int suffix = Integer.valueOf(alias.substring(alias.lastIndexOf("_") + 1))
                    .intValue();
                if (suffix >= max) {
                    max = suffix + 1;
                }
            }
        }
        return prefix + "_" + max;
    }

    /**
     * Convert a class name to a alias prefix
     * @param type the class name
     * @return a suitable prefix for an alias
     */
    protected static String toAlias(String type) {
        return TypeUtil.unqualifiedName(type);
    }

    /**
     * Add a new DisplayQueryClass of type className to the current query
     * @param queryClasses the exiting queryClasses
     * @param className the class name
     */
    protected static void addClass(Map queryClasses, String className) {
        DisplayQueryClass d = new DisplayQueryClass();
        d.setType(className);

        String alias = aliasClass(queryClasses.keySet(), className);
        queryClasses.put(alias, d);
    }
    
    /**
     * Create a Query from a Collection of DisplayQueryClasses
     * @param queryClasses the DisplayQueryClasses
     * @param model the relevant metadata
     * @param savedBags the savedBags on the session
     * @return the Query
     * @throws Exception if an error occurs in constructing the Query
     */
    protected static Query createQuery(Map queryClasses, Model model, Map savedBags)
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
                ConstraintOp fieldOp = (ConstraintOp) d.getFieldOp(constraintName);
                Object fieldValue = d.getFieldValue(constraintName);
                FieldDescriptor fd = (FieldDescriptor) cld.getFieldDescriptorByName(fieldName);
                if (fd.isAttribute()) {
                    if (savedBags != null
                        && savedBags.containsKey(fieldValue)
                        && BagConstraint.VALID_OPS.contains(fieldOp)) {
                        Collection bag = (Collection) savedBags.get(fieldValue);
                        QueryHelper.addConstraint(q, fieldName, qc, fieldOp, bag);
                    } else {
                        QueryHelper.addConstraint(q, fieldName, qc,
                                                  fieldOp, new QueryValue(fieldValue));
                    }
                } else if (fd.isReference()) {
                    QueryReference qr = new QueryObjectReference(qc, fieldName);
                    QueryClass qc2 = (QueryClass) q.getReverseAliases().get(fieldValue);
                    QueryHelper.addConstraint(q, qc, new ContainsConstraint(qr, fieldOp, qc2));
                } else if (fd.isCollection()) {
                    QueryReference qr = new QueryCollectionReference(qc, fieldName);
                    QueryClass qc2 = (QueryClass) q.getReverseAliases().get(fieldValue);
                    QueryHelper.addConstraint(q, qc, new ContainsConstraint(qr, fieldOp, qc2));
                }
            }
        }
        return q;
    }

    /**
     * Populate a QueryBuildForm given an active DisplayQueryClass
     * @param qbf the form
     * @param d the class being edited
     */
    protected static void populateForm(QueryBuildForm qbf, DisplayQueryClass d) {
        for (Iterator i = d.getFieldOps().keySet().iterator(); i.hasNext();) {
            String constraintName = (String) i.next();
            qbf.setFieldOp(constraintName, ((ConstraintOp) d.getFieldOps().get(constraintName))
                           .getIndex());
            qbf.setFieldValue(constraintName, d.getFieldValues().get(constraintName).toString());
        }
    }
    
    /**
     * Take a ClassDescriptor and return a List of the names of all its fields
     * @param cld the ClassDescriptor
     * @return the relevant List
     */
    protected static List getAllFieldNames(ClassDescriptor cld) {
        List allFieldNames = new ArrayList();
        for (Iterator i = cld.getAllFieldDescriptors().iterator(); i.hasNext();) {
            String fieldName = ((FieldDescriptor) i.next()).getName();
            if (!fieldName.equals("id")) {
                allFieldNames.add(fieldName);
            }
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
            fieldOps.put(attr.getName(), QueryBuildHelper.mapOps(ops));
        }

        //references and collections - allow valid ops for containsconstraints
        Map opString = QueryBuildHelper.mapOps(ContainsConstraint.VALID_OPS);
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
     * Take a Query and convert each of its QueryClasses to a DisplayQueryClass
     * @param q the Query
     * @param savedBagsInverse Map used to resolve bag names
     * @return Map a map from QueryClass alias to corresponding DisplayQueryClass
     */
    protected static Map getQueryClasses(Query q, Map savedBagsInverse) {
        Map queryClasses = new LinkedHashMap();
        for (Iterator i = q.getFrom().iterator(); i.hasNext();) {
            FromElement fe = (FromElement) i.next();
            if (!(fe instanceof QueryClass)) {
                continue;
            }
            QueryClass qc = (QueryClass) fe;
            
            queryClasses.put((String) q.getAliases().get(qc),
                             QueryBuildHelper.toDisplayable(qc, q, savedBagsInverse));
        }
        return queryClasses;
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

    /**
     * Iterate through each reference field of a class, building up a map from field name to a list
     * of class aliases in the query that could be part of a contains constraint on that field
     * @param cld metadata for the active QueryClass
     * @param queryClasses the DisplayQueryClass map
     * @return the revelant Map
     * @throws Exception if an error occurs
     */
    protected static Map getValidAliases(ClassDescriptor cld, Map queryClasses) throws Exception {
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
                for (Iterator i = queryClasses.keySet().iterator(); i.hasNext();) {
                    String alias = (String) i.next();
                    DisplayQueryClass d = (DisplayQueryClass) queryClasses.get(alias);
                    Class thisType = Class.forName(d.getType());
                    if (type.isAssignableFrom(thisType)) {
                        aliases.add(alias);
                    }
                }
            }
        }

        return values;
    }
}
