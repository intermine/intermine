package org.flymine.objectstore.query.presentation;

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
import java.text.SimpleDateFormat;
import java.text.ParseException;

import org.flymine.objectstore.query.*;
import org.flymine.metadata.*;
import org.flymine.FlyMineException;

/**
 * Static methods to assist with query generation from front end.
 *
 * @author Richard Smith
 */
public class QueryCreator
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
        }

        try {
            // if QueryClass already on query, remove existing constraints and
            // generate again
            if (q.getFrom().contains(qc)) {
                removeConstraints(q, qc);
            } else {
                q.addFrom(qc);
            }
            addConstraint(q, generateConstraints(qc, fieldValues, fieldOps,
                                                 q.getReverseAliases(), model));
        } catch (Exception e) {
            throw new FlyMineException("Problem occurred adding class ("
                                       + qc.getType().getName() + ") to query: " + e);
        }
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
                Constraint c = null;
                if (field instanceof AttributeDescriptor) {
                    QueryField qf = new QueryField(qc, field.getName());
                    QueryValue qv = createQueryValue(qf.getType(), fieldValue);
                    c = new SimpleConstraint(qf, op, qv);
                } else if (field instanceof ReferenceDescriptor) {
                    QueryReference qr = new QueryObjectReference(qc, field.getName());
                    // queryclass implements fromelement and querynode
                    c = new ContainsConstraint(qr, op, (QueryClass) aliases.get(fieldValue));
                }
                constraints.addConstraint(c);
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
     * Remove all constraints related to a given QueryClass.
     *
     * @param q the query to remove constraints from
     * @param qc remove all constraints relating to this QueryClass
     * @throws Exception if failed to remove constraints
     */
    protected static void removeConstraints(Query q, QueryClass qc) throws Exception {
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

        Iterator iter = ConstraintListCreator.createList(q, qc).iterator();
        while (iter.hasNext()) {
            // TODO will be simplified when refactoring of ConstraintListCreator done
            Constraint constraint = ((AssociatedConstraint) iter.next()).getConstraint();
            cs.removeConstraint(constraint);
        }
    }


    /**
     * Create a QueryValue by parsing a string for the appropriate class type
     * (common java.lang complex types and java.util.Date supported).
     *
     * @param type java type of the QueryValue to be created
     * @param value string to be parsed for value
     * @return a new QueryValue
     * @throws IllegalArgumentException if type is not supported
     * @throws ParseException if an error occurs parsing date string
     */
    protected static QueryValue createQueryValue(Class type, String value)
        throws IllegalArgumentException, ParseException {
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
}
