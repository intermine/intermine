package org.flymine.objectstore.query;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;
import java.util.Iterator;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import org.flymine.metadata.*;
import org.flymine.FlyMineException;

/**
 * Static methods to assist with query generation from front end.
 *
 * @author Richard Smith
 */
public abstract class QueryHelper
{
    protected static final String DATE_FORMAT = "dd/MM/yyyy";

    /**
     * Add a QueryClass to the from list of query and add constraints
     * generated from a map of fields/values.
     *
     * @param q a query to add QueryClass and constraints to
     * @param qc QueryClass to add to query
     * @param fieldValues map of fieldname/value to build constraints from
     * @param fieldOps map of fieldname/operation to build constraints from
     * @param model the business model
     * @throws FlyMineException if an error occurs
     */
    public static void addToQuery(Query q, QueryClass qc, Map fieldValues,
                                  Map fieldOps, Model model) throws FlyMineException {
        if (q == null) {
            throw new NullPointerException("Query q parameter is null");
        } else if (qc == null) {
            throw new NullPointerException("QueryClass qc parameter is null");
        } else if (fieldValues == null) {
            throw new NullPointerException("fieldValues parameter is null");
        } else if (fieldOps == null) {
            throw new NullPointerException("fieldOps parameter is null");
        } else if (model == null) {
            throw new NullPointerException("model parameter is null");
        }

        try {
            // if QueryClass already on query, remove existing constraints and
            // generate again
            if (q.getFrom().contains(qc)) {
                removeConstraints(q, qc, false);
            } else {
                q.addFrom(qc);
                q.addToSelect(qc);
            }
            addConstraint(q, generateConstraints(qc, fieldValues, fieldOps,
                                                 q.getReverseAliases(), model));
        } catch (Exception e) {
            throw new FlyMineException("Problem occurred adding class ("
                                       + qc.getType().getName() + ") to query: " + e);
        }
    }

    /**
     * Remove a class from a query.  Currently only deletes constraints that
     * are directly associated with QueryClass, should use isRelatedTo(qc) to find
     * all possible constraints.  If qc not in query no Exception is thrown.
     *
     * @param q the query to remove QueryClass from
     * @param qc the QueryClass to remove
     * @throws Exception if anything goes wrong
     */
    public static void removeFromQuery(Query q, QueryClass qc) throws Exception {
        if (q == null) {
            throw new NullPointerException("Query q parameter is null");
        } else if (qc == null) {
            throw new NullPointerException("QueryClass qc parameter is null");
        }

        q.deleteFromSelect(qc);
        q.deleteFrom(qc);
        removeConstraints(q, qc, true);
    }


    /**
     * Generate ConstraintSet of SimpleConstraints for a QueryClass from a map of field/value pairs
     *
     * @param qc QueryClass to constrain
     * @param fieldValues map of fieldname/value to build constraints from
     * @param fieldOps map of fieldname/operation to build constraints from
     * @param aliases map of alias/QueryNode of items on query
     * @param model the business model
     * @return a populated ConstraintSet
     * @throws Exception if it goes wrong
     */
    protected static ConstraintSet generateConstraints(QueryClass qc, Map fieldValues,
                                                       Map fieldOps, Map aliases,
                                                       Model model) throws Exception {
        ConstraintSet constraints = new ConstraintSet(ConstraintSet.AND);
        ClassDescriptor cld = model.getClassDescriptorByName(qc.getType().getName());

        Iterator iter = fieldValues.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry fieldEntry = (Map.Entry) iter.next();
            String fieldValue = (String) fieldEntry.getValue();
            if (!"".equals(fieldValue)) {
                FieldDescriptor field = cld.getFieldDescriptorByName((String) fieldEntry.getKey());
                Integer opCode = Integer.valueOf((String) fieldOps.get(field.getName()));
                ConstraintOp op = ConstraintOp.getOpForIndex(opCode);
                if (field instanceof AttributeDescriptor) {
                    QueryField qf = new QueryField(qc, field.getName());
                    QueryValue qv = createQueryValue(qf.getType(), fieldValue);
                    constraints.addConstraint(new SimpleConstraint(qf, op, qv));
                } else if (field instanceof CollectionDescriptor) {
                    QueryReference qr = new QueryCollectionReference(qc, field.getName());
                    QueryClass qc2 = (QueryClass) aliases.get(fieldValue);
                    constraints.addConstraint(new ContainsConstraint(qr, op, qc2));
                } else if (field instanceof ReferenceDescriptor) {
                    QueryReference qr = new QueryObjectReference(qc, field.getName());
                    QueryClass qc2 = (QueryClass) aliases.get(fieldValue);
                    constraints.addConstraint(new ContainsConstraint(qr, op, qc2));
                }
            }
        }
        return constraints;
    }

    /**
     * Adds the constraints in a ConstraintSet to those present in a Query
     *
     * @param q the query in question
     * @param constraints the new constraints
     */
    protected static void addConstraint(Query q, ConstraintSet constraints) {
        if (q == null) {
            throw new NullPointerException("q cannot be null");
        }

        if (constraints == null) {
            throw new NullPointerException("constraints cannot be null");
        }

        if (constraints.getConstraints().size() > 0) {
            Constraint c = q.getConstraint();
            if (c == null) {
                q.setConstraint(constraints);
            }  else if (c instanceof ConstraintSet) {
                // add all constraints, avoid nesting ConstraintSets
                Iterator iter = constraints.getConstraints().iterator();
                while (iter.hasNext()) {
                    ((ConstraintSet) c).addConstraint((Constraint) iter.next());
                }
            }  else { // any other type of constraint
                constraints.addConstraint(c);
                q.setConstraint(constraints);
            }
        }
    }

    /**
     * Remove all constraints associated with or related to a given QueryClass.
     *
     * @param q the query to remove constraints from
     * @param qc remove all constraints relating to this QueryClass
     * @param related if true remove all related constraints, otherwise only
     * those associated with qc.
     * @throws Exception if failed to remove constraints
     */
    protected static void removeConstraints(Query q, QueryClass qc, boolean related)
        throws Exception {
        Constraint c = q.getConstraint();
        if (c == null) {
            return;
        }

        ConstraintSet cs;
        if (!(c instanceof ConstraintSet)) {
            cs = new ConstraintSet(ConstraintSet.AND);
            cs.addConstraint(c);
        } else {
            cs = (ConstraintSet) c;
        }

        List constraints = ConstraintHelper.createList(q);
        Iterator iter = ConstraintHelper.filter(constraints, qc, related).iterator();
        while (iter.hasNext()) {
            cs.removeConstraint((Constraint) iter.next());
        }
    }

    /**
     * Create a QueryValue by parsing a string for the appropriate class type
     * (common java.lang complex types and java.util.Date supported).
     *
     * @param type java type of the QueryValue to be created
     * @param value string to be parsed for value
     * @return a new QueryValue
     * @throws ParseException if an error occurs parsing date string
     */
    protected static QueryValue createQueryValue(Class type, String value)
        throws ParseException {
        QueryValue qv = null;
        if (type.equals(Integer.class)) {
            qv = new QueryValue(Integer.valueOf(value));
        } else if (type.equals(Float.class)) {
            qv = new QueryValue(Float.valueOf(value));
        } else if (type.equals(Double.class)) {
            qv = new QueryValue(Double.valueOf(value));
        } else if (type.equals(Long.class)) {
            qv = new QueryValue(Long.valueOf(value));
        } else if (type.equals(Short.class)) {
            qv = new QueryValue(Short.valueOf(value));
        } else if (type.equals(Boolean.class)) {
            qv = new QueryValue(Boolean.valueOf(value));
        } else if (type.equals(Date.class)) {
            qv = new QueryValue(new SimpleDateFormat(DATE_FORMAT).parse(value));
        } else if (type.equals(String.class)) {
            qv = new QueryValue(value);
        } else {
            throw new IllegalArgumentException("Invalid type for QueryValue: " + type);
        }
        return qv;
    }

    /**
     * Returns a list of aliases, where each alias corresponds to each element of the SELECT list
     * of the Query object. This is effectively a list of column headings for the results object.
     * @param query the Query object
     * @return a List of Strings, each of which is the alias of the column
     */
    public static List getColumnAliases(Query query) {
        List columnAliases = new ArrayList();
        Iterator selectIter = query.getSelect().iterator();
        while (selectIter.hasNext()) {
            QueryNode node = (QueryNode) selectIter.next();
            String alias = (String) query.getAliases().get(node);
            columnAliases.add(alias);
        }
        return columnAliases;
    }

    /**
     * Returns a list of Class objects, where each object corresponds to the type of each element
     * of the SELECT list of the Query object. This is effectively a list of column types for the
     * results object.
     * @param query the Query object
     * @return a List of Class objects
     */
    public static List getColumnTypes(Query query) {
        List columnTypes = new ArrayList();
        Iterator selectIter = query.getSelect().iterator();
        while (selectIter.hasNext()) {
            QueryNode node = (QueryNode) selectIter.next();
            Class type = node.getType();
            columnTypes.add(type);
        }
        return columnTypes;
    }
}
