package org.flymine.objectstore.flymine;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

import org.flymine.metadata.AttributeDescriptor;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.CollectionDescriptor;
import org.flymine.metadata.FieldDescriptor;
import org.flymine.metadata.Model;
import org.flymine.metadata.ReferenceDescriptor;
import org.flymine.model.FlyMineBusinessObject;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryNode;
import org.flymine.objectstore.query.QueryEvaluable;
import org.flymine.objectstore.query.QueryField;
import org.flymine.objectstore.query.QueryExpression;
import org.flymine.objectstore.query.QueryFunction;
import org.flymine.objectstore.query.QueryValue;
import org.flymine.objectstore.query.FromElement;
import org.flymine.objectstore.query.Constraint;
import org.flymine.objectstore.query.ConstraintOp;
import org.flymine.objectstore.query.ConstraintSet;
import org.flymine.objectstore.query.SimpleConstraint;
import org.flymine.objectstore.query.SubqueryConstraint;
import org.flymine.objectstore.query.ClassConstraint;
import org.flymine.objectstore.query.ContainsConstraint;
import org.flymine.objectstore.query.BagConstraint;
import org.flymine.objectstore.query.QueryReference;
import org.flymine.objectstore.query.QueryObjectReference;
import org.flymine.objectstore.query.QueryCollectionReference;
import org.flymine.objectstore.query.UnknownTypeValue;
import org.flymine.util.DatabaseUtil;
import org.flymine.util.TypeUtil;

import org.apache.log4j.Logger;

/**
 * Code to generate an sql statement from a Query object.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 * @author Richard Smith
 */
public class SqlGenerator
{
    protected static final Logger LOG = Logger.getLogger(SqlGenerator.class);
    protected static final int QUERY_NORMAL = 0;
    protected static final int QUERY_SUBQUERY_FROM = 1;
    protected static final int QUERY_SUBQUERY_CONSTRAINT = 2;
    protected static final int ID_ONLY = 2;
    protected static final int NO_ALIASES_ALL_FIELDS = 3;

    /**
     * Converts a Query object into an SQL String. To produce an SQL query that does not have
     * OFFSET and LIMIT clauses, set start to 0, and limit to Integer.MAX_VALUE.
     *
     * @param q the Query to convert
     * @param start the number of the first row for the query to return, numbered from zero
     * @param limit the maximum number of rows for the query to return
     * @param model the Model to look up metadata in
     * @return a String suitable for passing to an SQL server
     */
    public static String generate(Query q, int start, int limit, Model model) 
            throws ObjectStoreException {
        return generate(q, start, limit, model, QUERY_NORMAL);
    }

    /**
     * Converts a Query object into an SQL String. To produce an SQL query that does not have
     * OFFSET and LIMIT clauses, set start to 0, and limit to Integer.MAX_VALUE.
     *
     * @param q the Query to convert
     * @param start the number of the first row for the query to return, numbered from zero
     * @param limit the maximum number of rows for the query to return
     * @param model the Model to look up metadata in
     * @param kind Query type
     * @return a String suitable for passing to an SQL server
     */
    protected static String generate(Query q, int start, int limit, Model model,
            int kind) throws ObjectStoreException {
        State state = new State();
        buildFromComponent(state, q, model);
        buildWhereClause(state, q, model);
        String orderBy = (kind == QUERY_NORMAL ? buildOrderBy(state, q, model) : "");
        return "SELECT " + (q.isDistinct() ? "DISTINCT " : "")
            + buildSelectComponent(state, q, model, kind) + state.getFrom() + state.getWhere()
            + buildGroupBy(q, model) + orderBy
            + (limit == Integer.MAX_VALUE ? "" : " LIMIT " + limit)
            + (start == 0 ? "" : " OFFSET " + start);
    }

    /**
     * Builds the FROM list for the SQL query.
     *
     * @param state the current Sql Query state
     * @param q the Query
     * @param model the Model
     */
    protected static void buildFromComponent(State state, Query q, Model model)
            throws ObjectStoreException {
        Set fromElements = q.getFrom();
        Iterator fromIter = fromElements.iterator();
        while (fromIter.hasNext()) {
            FromElement fromElement = (FromElement) fromIter.next();
            if (fromElement instanceof QueryClass) {
                ClassDescriptor cld = model.getClassDescriptorByName(
                        ((QueryClass) fromElement).getType().getName());
                if (cld == null) {
                    throw new ObjectStoreException(((QueryClass) fromElement).getType().toString()
                            + " is not in the model");
                }
                state.addToFrom(DatabaseUtil.getTableName(cld) + " AS "
                        + ((String) q.getAliases().get(fromElement)));
            } else if (fromElement instanceof Query) {
                state.addToFrom("(" + generate((Query) fromElement, 0, Integer.MAX_VALUE,
                                model, QUERY_SUBQUERY_FROM) + ") AS "
                        + ((String) q.getAliases().get(fromElement)));
            }
        }
    }

    /**
     * Builds the WHERE clause for the SQL query.
     *
     * @param state the current Sql Query state
     * @param q the Query
     * @param model the Model
     */
    protected static void buildWhereClause(State state, Query q, Model model)
            throws ObjectStoreException {
        Constraint c = q.getConstraint();
        if (c != null) {
            if (state.getWhereBuffer().length() > 0) {
                state.addToWhere(" AND ");
            }
            constraintToString(state, c, q, model);
        }
    }

    /**
     * Converts a Constraint object into a String suitable for putting in an SQL query.
     *
     * @param state the object to place text into
     * @param c the Constraint object
     * @param q the Query
     * @param model the Model
     */
    protected static void constraintToString(State state, Constraint c, Query q, Model model)
            throws ObjectStoreException {
        if (c instanceof ConstraintSet) {
            constraintSetToString(state, (ConstraintSet) c, q, model);
        } else if (c instanceof SimpleConstraint) {
            simpleConstraintToString(state, (SimpleConstraint) c, q, model);
        } else if (c instanceof SubqueryConstraint) {
            subqueryConstraintToString(state, (SubqueryConstraint) c, q, model);
        } else if (c instanceof ClassConstraint) {
            classConstraintToString(state, (ClassConstraint) c, q, model);
        } else if (c instanceof ContainsConstraint) {
            containsConstraintToString(state, (ContainsConstraint) c, q, model);
        } else if (c instanceof BagConstraint) {
            bagConstraintToString(state, (BagConstraint) c, q, model);
        } else {
            throw (new IllegalArgumentException("Unknown constraint type: " + c));
        }
    }

    /**
     * Converts a ConstraintSet object into a String suitable for putting in an SQL query.
     *
     * @param state the object to place text into
     * @param c the ConstraintSet object
     * @param q the Query
     * @param model the Model
     */
    protected static void constraintSetToString(State state, ConstraintSet c, Query q, Model model)
            throws ObjectStoreException {
        ConstraintOp op = c.getOp();
        boolean negate = (op == ConstraintOp.NAND) || (op == ConstraintOp.NOR);
        boolean disjunctive = (op == ConstraintOp.OR) || (op == ConstraintOp.NOR);
        if (c.getConstraints().isEmpty()) {
            state.addToWhere((disjunctive ? negate : !negate) ? "true" : "false");
        } else {
            state.addToWhere(negate ? "( NOT (" : "(");
            boolean needComma = false;
            Set constraints = c.getConstraints();
            Iterator constraintIter = constraints.iterator();
            while (constraintIter.hasNext()) {
                Constraint subC = (Constraint) constraintIter.next();
                if (needComma) {
                    state.addToWhere(disjunctive ? " OR " : " AND ");
                }
                needComma = true;
                constraintToString(state, subC, q, model);
            }
            state.addToWhere(negate ? "))" : ")");
        }
    }

    /**
     * Converts a SimpleConstraint object into a String suitable for putting in an SQL query.
     *
     * @param state the object to place text into
     * @param c the SimpleConstraint object
     * @param q the Query
     * @param model the Model
     */
    protected static void simpleConstraintToString(State state, SimpleConstraint c, Query q,
            Model model) throws ObjectStoreException {
        queryEvaluableToString(state.getWhereBuffer(), c.getArg1(), q);
        state.addToWhere(" " + c.getOp().toString());
        if (c.getArg2() != null) {
            state.addToWhere(" ");
            queryEvaluableToString(state.getWhereBuffer(), c.getArg2(), q);
        }
    }

    /**
     * Converts a SubqueryConstraint object into a String suitable for putting in an SQL query.
     *
     * @param state the object to place text into
     * @param c the SubqueryConstraint object
     * @param q the Query
     * @param model the Model
     */
    protected static void subqueryConstraintToString(State state, SubqueryConstraint c, Query q,
            Model model) throws ObjectStoreException {
        Query subQ = c.getQuery();
        QueryEvaluable qe = c.getQueryEvaluable();
        QueryClass cls = c.getQueryClass();
        if (qe != null) {
            queryEvaluableToString(state.getWhereBuffer(), qe, q);
        } else {
            queryClassToString(state.getWhereBuffer(), cls, q, model, QUERY_SUBQUERY_CONSTRAINT);
        }
        state.addToWhere(" " + c.getOp().toString() + " (" + generate(subQ, 0, Integer.MAX_VALUE,
                        model, QUERY_SUBQUERY_CONSTRAINT) + ")");
    }

    /**
     * Converts a ClassConstraint object into a String suitable for putting in an SQL query.
     *
     * @param state the object to place text into
     * @param c the ClassConstraint object
     * @param q the Query
     * @param model the Model
     */
    protected static void classConstraintToString(State state, ClassConstraint c, Query q,
            Model model) throws ObjectStoreException {
        QueryClass arg1 = c.getArg1();
        String alias1 = ((String) q.getAliases().get(arg1)) + ".";
        QueryClass arg2QC = c.getArg2QueryClass();
        String alias2 = null;
        if (arg2QC != null) {
            alias2 = ((String) q.getAliases().get(arg2QC)) + ".";
        }
        FlyMineBusinessObject arg2O = c.getArg2Object();
        queryClassToString(state.getWhereBuffer(), arg1, q, model, ID_ONLY);
        state.addToWhere(" " + c.getOp().toString() + " ");
        if (arg2QC != null) {
            queryClassToString(state.getWhereBuffer(), arg2QC, q, model, ID_ONLY);
        } else if (arg2O.getId() != null) {
            objectToString(state.getWhereBuffer(), arg2O);
        } else {
            throw new ObjectStoreException("ClassConstraint cannot contain a FlyMineBusinessObject"
                    + " without an ID set");
        }
    }

    /**
     * Converts a ContainsConstraint object into a String suitable for putting in an SQL query.
     *
     * @param state the object to place text into
     * @param c the ContainsConstraint object
     * @param q the Query
     * @param model the Model
     */
    protected static void containsConstraintToString(State state, ContainsConstraint c,
            Query q, Model model) throws ObjectStoreException {
        QueryReference arg1 = c.getReference();
        QueryClass arg2 = c.getQueryClass();
        ClassDescriptor arg1Class = model.getClassDescriptorByName(arg1.getQueryClass().getType()
                .getName());
        if (arg1Class == null) {
            throw new ObjectStoreException(arg1.getQueryClass().getType().toString()
                    + " is not in the model");
        }
        ReferenceDescriptor arg1Desc = (ReferenceDescriptor) arg1Class
            .getFieldDescriptorByName(arg1.getFieldName());
        String arg1Alias = ((String) q.getAliases().get(arg1.getQueryClass()));
        if (arg1 instanceof QueryObjectReference) {
            state.addToWhere(arg1Alias + "." + DatabaseUtil.getColumnName(arg1Desc)
                    + (c.getOp() == ConstraintOp.CONTAINS ? " = " : " != "));
            queryClassToString(state.getWhereBuffer(), arg2, q, model, ID_ONLY);
        } else if (arg1 instanceof QueryCollectionReference) {
            ClassDescriptor arg2Class = model.getClassDescriptorByName(arg2.getType().getName());
            if (arg2Class == null) {
                throw new ObjectStoreException(arg2.getType().toString()
                        + " is not in the model");
            }
            String arg2Alias = ((String) q.getAliases().get(arg2));
            if (arg1Desc.relationType() == FieldDescriptor.ONE_N_RELATION) {
                queryClassToString(state.getWhereBuffer(), arg1.getQueryClass(), q, model, ID_ONLY);
                state.addToWhere((c.getOp() == ConstraintOp.CONTAINS ? " = " : " != ") + arg2Alias + "."
                        + DatabaseUtil.getColumnName(arg1Desc.getReverseReferenceDescriptor()));
            } else {
                CollectionDescriptor arg1ColDesc = (CollectionDescriptor) arg1Desc;
                String indirectTableAlias = state.getIndirectAlias();
                state.addToFrom(DatabaseUtil.getIndirectionTableName(arg1ColDesc) + " AS "
                        + indirectTableAlias);
                state.addToWhere(c.getOp().equals(ConstraintOp.CONTAINS) ? "(" : "( NOT (");
                queryClassToString(state.getWhereBuffer(), arg1.getQueryClass(), q, model, ID_ONLY);
                state.addToWhere(" = " + indirectTableAlias + "."
                        + DatabaseUtil.getInwardIndirectionColumnName(arg1ColDesc) + " AND "
                        + indirectTableAlias + "."
                        + DatabaseUtil.getOutwardIndirectionColumnName(arg1ColDesc) + " = ");
                queryClassToString(state.getWhereBuffer(), arg2, q, model, ID_ONLY);
                state.addToWhere(c.getOp().equals(ConstraintOp.CONTAINS) ? ")" : "))");
            }
        }
    }

    /**
     * Converts a BagConstraint object into a String suitable for putting on an SQL query.
     *
     * @param state the object to place text into
     * @param c the BagConstraint object
     * @param q the Query
     * @param model the Model
     */
    protected static void bagConstraintToString(State state, BagConstraint c, Query q,
            Model model) throws ObjectStoreException {
        Class type = c.getQueryNode().getType();
        String leftHandSide;
        if (c.getQueryNode() instanceof QueryEvaluable) {
            StringBuffer lhsBuffer = new StringBuffer();
            queryEvaluableToString(lhsBuffer, (QueryEvaluable) c.getQueryNode(), q);
            leftHandSide = lhsBuffer.toString() + " = ";
        } else {
            StringBuffer lhsBuffer = new StringBuffer();
            queryClassToString(lhsBuffer, (QueryClass) c.getQueryNode(), q, model, ID_ONLY);
            leftHandSide = lhsBuffer.toString() + " = ";
        }
        SortedSet filteredBag = new TreeSet();
        Iterator bagIter = c.getBag().iterator();
        while (bagIter.hasNext()) {
            Object bagItem = bagIter.next();
            if (type.isInstance(bagItem)) {
                StringBuffer constraint = new StringBuffer(leftHandSide);
                objectToString(constraint, bagItem);
                filteredBag.add(constraint.toString());
            }
        }
        if (filteredBag.isEmpty()) {
            state.addToWhere(c.getOp() == ConstraintOp.IN ? "false" : "true");
        } else {
            boolean needComma = false;
            Iterator orIter = filteredBag.iterator();
            while (orIter.hasNext()) {
                state.addToWhere(needComma ? " OR " : (c.getOp() == ConstraintOp.IN ? "("
                            : "( NOT ("));
                needComma = true;
                state.addToWhere((String) orIter.next());
            }
            state.addToWhere(c.getOp() == ConstraintOp.IN ? ")" : "))");
        }
    }

    /**
     * Converts an Object to a String, in a form suitable for SQL.
     *
     * @param buffer a StringBuffer to add text to
     * @param obj the Object to convert
     */
    public static void objectToString(StringBuffer buffer, Object value)
            throws ObjectStoreException {
        if (value instanceof Date) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            buffer.append("'" + format.format((Date) value) + "'");
        } else if (value instanceof Float) {
            buffer.append(value.toString() + "::REAL");
        } else if (value instanceof Number) {
            buffer.append(value.toString());
        } else if (value instanceof String) {
            buffer.append("'" + value + "'");
        } else if (value instanceof Boolean) {
            buffer.append(((Boolean) value).booleanValue() ? "'true'" : "'false'");
        } else if (value instanceof UnknownTypeValue) {
            buffer.append(value.toString());
        } else if (value instanceof FlyMineBusinessObject) {
            Integer id = ((FlyMineBusinessObject) value).getId();
            if (id == null) {
                throw new ObjectStoreException("FlyMineBusinessObject found"
                        + " without an ID set");
            }
            buffer.append(id.toString());
        } else {
            throw (new IllegalArgumentException("Invalid Object in QueryValue: "
                                                + value));
        }
    }

    /**
     * Converts a QueryClass to a String.
     * 
     * @param buffer the StringBuffer to add text to
     * @param qc the QueryClass to convert
     * @param q the Query
     * @param model the Model
     * @param kind the type of the output requested
     */
    protected static void queryClassToString(StringBuffer buffer, QueryClass qc, Query q,
            Model model, int kind) {
        String alias = (String) q.getAliases().get(qc);
        if (kind == QUERY_SUBQUERY_CONSTRAINT) {
            buffer.append(alias)
                .append(".id");
        } else {
            buffer.append(alias)
                .append(".OBJECT");
            if ((kind == QUERY_NORMAL) || (kind == QUERY_SUBQUERY_FROM)) {
                buffer.append(" AS ")
                    .append(alias.equals(alias.toLowerCase()) ? alias : "\"" + alias + "\"");
            }
            if ((kind == QUERY_SUBQUERY_FROM) || (kind == NO_ALIASES_ALL_FIELDS)) {
                Set fields = model.getClassDescriptorByName(qc.getType().getName())
                    .getAllFieldDescriptors();
                Map fieldMap = new TreeMap();
                Iterator fieldIter = fields.iterator();
                while (fieldIter.hasNext()) {
                    FieldDescriptor field = (FieldDescriptor) fieldIter.next();
                    String columnName = DatabaseUtil.getColumnName(field);
                    if (columnName != null) {
                        fieldMap.put(columnName, field);
                    }
                }
                Iterator fieldMapIter = fieldMap.entrySet().iterator();
                while (fieldMapIter.hasNext()) {
                    Map.Entry fieldEntry = (Map.Entry) fieldMapIter.next();
                    FieldDescriptor field = (FieldDescriptor) fieldEntry.getValue();
                    String columnName = DatabaseUtil.getColumnName(field);

                    buffer.append(", ")
                        .append(alias)
                        .append(".")
                        .append(columnName);
                    if (kind == QUERY_SUBQUERY_FROM) {
                        buffer.append(" AS ")
                            .append(alias.equals(alias.toLowerCase()) ? alias + columnName
                                    : "\"" + alias + columnName + "\"");
                    }
                }
            } else {
                buffer.append(", ")
                    .append(alias)
                    .append(".id AS ")
                    .append(alias.equals(alias.toLowerCase()) ? alias + "id" : "\"" + alias
                            + "id" + "\"");
            }
        }
    }

    /**
     * Converts a QueryEvaluable into a String suitable for an SQL query String.
     *
     * @param buffer the StringBuffer to add text to
     * @param node the QueryEvaluable
     * @param q the Query
     */
    protected static void queryEvaluableToString(StringBuffer buffer, QueryEvaluable node,
            Query q) throws ObjectStoreException {
        if (node instanceof QueryField) {
            QueryField nodeF = (QueryField) node;
            FromElement nodeClass = nodeF.getFromElement();
            String classAlias = (String) q.getAliases().get(nodeClass);

            buffer.append(classAlias)
                .append(".")
                .append(nodeF.getFieldName())
                .append(nodeF.getSecondFieldName() == null ? "" : nodeF.getSecondFieldName());
        } else if (node instanceof QueryExpression) {
            QueryExpression nodeE = (QueryExpression) node;
            if (nodeE.getOperation() == QueryExpression.SUBSTRING) {
                QueryEvaluable arg1 = nodeE.getArg1();
                QueryEvaluable arg2 = nodeE.getArg2();
                QueryEvaluable arg3 = nodeE.getArg3();

                buffer.append("Substr(");
                queryEvaluableToString(buffer, arg1, q);
                buffer.append(", ");
                queryEvaluableToString(buffer, arg2, q);
                buffer.append(", ");
                queryEvaluableToString(buffer, arg3, q);
                buffer.append(")");
            } else {
                QueryEvaluable arg1 = nodeE.getArg1();
                QueryEvaluable arg2 = nodeE.getArg2();
                String op = null;
                switch (nodeE.getOperation()) {
                    case QueryExpression.ADD:
                        op = " + ";
                        break;
                    case QueryExpression.SUBTRACT:
                        op = " - ";
                        break;
                    case QueryExpression.MULTIPLY:
                        op = " * ";
                        break;
                    case QueryExpression.DIVIDE:
                        op = " / ";
                        break;
                    default:
                        throw (new IllegalArgumentException("Invalid QueryExpression operation: "
                                                            + nodeE.getOperation()));
                }
                buffer.append("(");
                queryEvaluableToString(buffer, arg1, q);
                buffer.append(op);
                queryEvaluableToString(buffer, arg2, q);
                buffer.append(")");
            }
        } else if (node instanceof QueryFunction) {
            QueryFunction nodeF = (QueryFunction) node;
            switch (nodeF.getOperation()) {
            case QueryFunction.COUNT:
                buffer.append("COUNT(*)");
                break;
            case QueryFunction.SUM:
                buffer.append("SUM(");
                queryEvaluableToString(buffer, nodeF.getParam(), q);
                buffer.append(")");
                break;
            case QueryFunction.AVERAGE:
                buffer.append("AVG(");
                queryEvaluableToString(buffer, nodeF.getParam(), q);
                buffer.append(")");
                break;
            case QueryFunction.MIN:
                buffer.append("MIN(");
                queryEvaluableToString(buffer, nodeF.getParam(), q);
                buffer.append(")");
                break;
            case QueryFunction.MAX:
                buffer.append("MAX(");
                queryEvaluableToString(buffer, nodeF.getParam(), q);
                buffer.append(")");
                break;
            default:
                throw (new IllegalArgumentException("Invalid QueryFunction operation: "
                                                    + nodeF.getOperation()));
            }
        } else if (node instanceof QueryValue) {
            QueryValue nodeV = (QueryValue) node;
            Object value = nodeV.getValue();
            objectToString(buffer, value);
        } else {
            throw (new IllegalArgumentException("Invalid QueryEvaluable: " + node));
        }
    }

    /**
     * Builds a String representing the SELECT component of the Sql query.
     *
     * @param state the current Sql Query state
     * @param q the Query
     * @param model the Model
     * @param kind the kind of output requested
     * @return a String
     */
    protected static String buildSelectComponent(State state, Query q, Model model, int kind)
            throws ObjectStoreException {
        boolean needComma = false;
        StringBuffer retval = new StringBuffer();
        Iterator iter = q.getSelect().iterator();
        while (iter.hasNext()) {
            QueryNode node = (QueryNode) iter.next();
            if (needComma) {
                retval.append(", ");
            }
            needComma = true;
            if (node instanceof QueryClass) {
                queryClassToString(retval, (QueryClass) node, q, model, kind);
            } else if (node instanceof QueryEvaluable) {
                queryEvaluableToString(retval, (QueryEvaluable) node, q);
                String alias = (String) q.getAliases().get(node);
                if ((kind == QUERY_NORMAL) || (kind == QUERY_SUBQUERY_FROM)) {
                    retval.append(" AS " + (alias.equals(alias.toLowerCase()) ? alias : "\"" + alias
                                + "\""));
                }
            }
        }
        iter = state.getOrderBy().iterator();
        while (iter.hasNext()) {
            String orderByField = (String) iter.next();
            if (needComma) {
                retval.append(", ");
            }
            needComma = true;
            retval.append(orderByField);
        }
        return retval.toString();
    }

    /**
     * Builds a String representing the GROUP BY component of the Sql query.
     *
     * @param q the Query
     * @param model the Model
     * @return a String
     */
    protected static String buildGroupBy(Query q, Model model) throws ObjectStoreException {
        StringBuffer retval = new StringBuffer();
        boolean needComma = false;
        Iterator groupByIter = q.getGroupBy().iterator();
        while (groupByIter.hasNext()) {
            QueryNode node = (QueryNode) groupByIter.next();
            retval.append(needComma ? ", " : " GROUP BY ");
            needComma = true;
            if (node instanceof QueryClass) {
                queryClassToString(retval, (QueryClass) node, q, model, NO_ALIASES_ALL_FIELDS);
            } else {
                queryEvaluableToString(retval, (QueryEvaluable) node, q);
            }
        }
        return retval.toString();
    }

    /**
     * Builds a String representing the ORDER BY component of the Sql query.
     *
     * @param state the current Sql Query state
     * @param q the Query
     * @param model the Model
     * @return a String
     */
    protected static String buildOrderBy(State state, Query q, Model model) throws ObjectStoreException {
        StringBuffer retval = new StringBuffer();
        boolean needComma = false;
        List orderBy = new ArrayList(q.getOrderBy());
        orderBy.addAll(q.getSelect());
        Iterator orderByIter = orderBy.iterator();
        while (orderByIter.hasNext()) {
            QueryNode node = (QueryNode) orderByIter.next();
            if (!(node instanceof QueryValue)) {
                retval.append(needComma ? ", " : " ORDER BY ");
                needComma = true;
                if (node instanceof QueryClass) {
                    queryClassToString(retval, (QueryClass) node, q, model, ID_ONLY);
                } else {
                    queryEvaluableToString(retval, (QueryEvaluable) node, q);
                    if (!q.getSelect().contains(node)) {
                        StringBuffer buffer = new StringBuffer();
                        queryEvaluableToString(buffer, (QueryEvaluable) node, q);
                        buffer.append(" AS ")
                            .append(state.getOrderByAlias());
                        state.addToOrderBy(buffer.toString());
                    }
                }
            }
        }
        return retval.toString();
    }

    private static class State {
        private StringBuffer whereText = new StringBuffer();
        private StringBuffer fromText = new StringBuffer();
        private Set orderBy = new LinkedHashSet();
        private int number = 0;

        public State() {
        }

        public String getWhere() {
            return (whereText.length() == 0 ? "" : " WHERE " + whereText.toString());
        }

        public StringBuffer getWhereBuffer() {
            return whereText;
        }

        public String getFrom() {
            return fromText.toString();
        }

        public void addToWhere(String text) {
            whereText.append(text);
        }

        public void addToFrom(String text) {
            if (fromText.length() == 0) {
                fromText.append(" FROM ").append(text);
            } else {
                fromText.append(", ").append(text);
            }
        }

        public String getIndirectAlias() {
            return "indirect" + (number++);
        }

        public String getOrderByAlias() {
            return "orderbyfield" + (number++);
        }

        public void addToOrderBy(String s) {
            orderBy.add(s);
        }

        public Set getOrderBy() {
            return orderBy;
        }
    }
}


