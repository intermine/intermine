package org.flymine.objectstore.ojb;


/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 *    "Apache ObjectRelationalBridge" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache ObjectRelationalBridge", nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

//import java.util.HashSet;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.TreeSet;
import java.util.Set;

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
import org.flymine.objectstore.query.ConstraintSet;
import org.flymine.objectstore.query.SimpleConstraint;
import org.flymine.objectstore.query.SubqueryConstraint;
import org.flymine.objectstore.query.ClassConstraint;


import org.apache.ojb.broker.accesslayer.sql.SqlStatement;
import org.apache.ojb.broker.accesslayer.conversions.Boolean2IntFieldConversion;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.DescriptorRepository;

/**
 * Code to generate and sql statement...
 *
 * @author Richard Smith
 * @author Matthew Wakeling
 */


public class FlymineSqlSelectStatement implements SqlStatement
{
    private Query query;
    private DescriptorRepository dr;
    private int start;
    private int limit;
    private boolean isSubQuery;
    private boolean isAConstraint;

    /**
     * Constructor requires a FlyMine Query and associated array of ClassDescriptors.
     * Should be a ClassDescriptor for each class in FROM clause of query.
     *
     * @param query a flymine query
     * @param dr DescriptorRepository for the database
     * @param start the number of rows to skip at the beginning
     * @param limit the maximum number of rows to return
     */
    public FlymineSqlSelectStatement(Query query, DescriptorRepository dr, int start, int limit) {
        this.query = query;
        this.dr = dr;
        this.start = start;
        this.limit = limit;
        this.isSubQuery = false;
    }

    /**
     * Constructor requires a FlyMine Query and associated array of ClassDescriptors.
     * Should be a ClassDescriptor for each class in FROM clause of query.
     *
     * @param query a flymine query
     * @param dr DescriptorRepository for the database
     */
    public FlymineSqlSelectStatement(Query query, DescriptorRepository dr) {
        this(query, dr, false);
    }

    /**
     * Constructor requires a FlyMine Query and associated array of ClassDescriptors.
     * Should be a ClassDescriptor for each class in FROM clause of query.
     *
     * @param query a flymine query
     * @param dr DescriptorRepository for the database
     * @param isAConstraint true if this is a query that is part of a subquery constraint
     */
    public FlymineSqlSelectStatement(Query query, DescriptorRepository dr, boolean isAConstraint) {
        this.query = query;
        this.dr = dr;
        this.start = 0;
        this.limit = 0;
        this.isSubQuery = true;
        this.isAConstraint = isAConstraint;
    }

    /**
     * Returns a String containing the entire SELECT list of the query.
     *
     * @return the SELECT list
     */
    protected String buildSelectComponent() {
        String retval = "";
        boolean needComma = false;
        List select = query.getSelect();
        Iterator selectIter = select.iterator();
        while (selectIter.hasNext()) {
            QueryNode node = (QueryNode) selectIter.next();
            if (needComma) {
                retval += ", ";
            }
            needComma = true;
            if (node instanceof QueryClass) {
                retval += queryClassToString((QueryClass) node, true, isAConstraint);
            } else {
                retval += queryEvaluableToString((QueryEvaluable) node) + " AS "
                    + query.getAliases().get(node);
            }
        }
        return retval;
    }

    /**
     * Converts a QueryClass into the SELECT list fields required to represent it in the SQL query.
     *
     * @param node the QueryClass
     * @param aliases whether to include aliases in the field list
     * @param primaryOnly whether to only list primary keys
     * @return the String representation
     */
    protected String queryClassToString(QueryClass node, boolean aliases, boolean primaryOnly) {
        String retval = "";
        boolean needComma = false;
        // It's a class - find its class descriptor, then iterate through its fields.
        // This QueryClass should be aliased as described by Query.getAliases().
        String alias = (String) query.getAliases().get(node);
        boolean done = false;
        ClassDescriptor cld = dr.getDescriptorFor(node.getType());
        if (cld == null) {
            throw (new IllegalArgumentException("Couldn't find class descriptor for class "
                        + node.getType()));
        }
        // Now cld is the ClassDescriptor of the node, and alias is the alias
        FieldDescriptor fields[] = (primaryOnly ? cld.getPkFields() : cld.getFieldDescriptions());
        TreeSet fieldnames = new TreeSet();
        for (int i = 0; i < fields.length; i++) {
            FieldDescriptor field = fields[i];
            fieldnames.add(field.getColumnName());
        }
        Iterator fieldnameIter = fieldnames.iterator();
        while (fieldnameIter.hasNext()) {
            String fieldname = (String) fieldnameIter.next();
            if (needComma) {
                retval += ", ";
            }
            needComma = true;
            retval += alias + "." + fieldname + (aliases ? " AS " + alias + fieldname : "");
        }
        return retval;
    }

    /**
     * Converts a QueryEvaluable into a SELECT list field.
     *
     * @param node the QueryEvaluable
     * @return the String representation
     */
    protected String queryEvaluableToString(QueryEvaluable node) {
        if (node instanceof QueryField) {
            // It's a field - find its FieldDescriptor by looking at its QueryClass, then its
            // ClassDescriptor.
            QueryField nodeF = (QueryField) node;
            FromElement nodeClass = nodeF.getFromElement();
            String classAlias = (String) query.getAliases().get(nodeClass);
            //boolean done = false;
            //ClassDescriptor cld = null;
            //for (int i = 0; (i<clds.length) && (! done) ; i++) {
            //    cld = clds[i];
            //    if (cld.getClassOfObject().equals(nodeClass.getType())) {
            //        done = true;
            //    }
            //}
            //if (cld == null) {
            //    throw (new Exception("Couldn't find class descriptor for class "
            //                + nodeClass.getType()));
            //}

            // Now cld is the ClassDescriptor for the node's class. Now need to find node's
            // FieldDescriptor.

            return classAlias + "." + nodeF.getFieldName();
        } else if (node instanceof QueryExpression) {
            QueryExpression nodeE = (QueryExpression) node;
            if (nodeE.getOperation() == QueryExpression.SUBSTRING) {
                QueryEvaluable arg1 = nodeE.getArg1();
                QueryEvaluable arg2 = nodeE.getArg2();
                QueryEvaluable arg3 = nodeE.getArg3();

                return "Substr(" + queryEvaluableToString(arg1) + ", "
                    + queryEvaluableToString(arg2) + ", " + queryEvaluableToString(arg3) + ")";
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
                return "(" + queryEvaluableToString(arg1) + op + queryEvaluableToString(arg2) + ")";
            }
        } else if (node instanceof QueryFunction) {
            QueryFunction nodeF = (QueryFunction) node;
            switch (nodeF.getOperation()) {
                case QueryFunction.COUNT:
                    return "COUNT(*)";
                case QueryFunction.SUM:
                    return "SUM(" + queryEvaluableToString(nodeF.getParam()) + ")";
                case QueryFunction.AVERAGE:
                    return "AVG(" + queryEvaluableToString(nodeF.getParam()) + ")";
                case QueryFunction.MIN:
                    return "MIN(" + queryEvaluableToString(nodeF.getParam()) + ")";
                case QueryFunction.MAX:
                    return "MAX(" + queryEvaluableToString(nodeF.getParam()) + ")";
                default:
                    throw (new IllegalArgumentException("Invalid QueryFunction operation: "
                                + nodeF.getOperation()));
            }
        } else if (node instanceof QueryValue) {
            QueryValue nodeV = (QueryValue) node;
            Object value = nodeV.getValue();
            return objectToString(value);
        } else {
            throw (new IllegalArgumentException("Invalid QueryEvaluable: " + node.toString()));
        }
    }

    /**
     * Converts an Object into a SQL String.
     *
     * @param value the object to convert
     * @return the String representation
     */
    public static String objectToString(Object value) {
        if (value instanceof Date) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            return "'" + format.format((Date) value) + "'";
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof String) {
            return "'" + value + "'";
        } else if (value instanceof Boolean) {
            return (new Boolean2IntFieldConversion()).javaToSql(value).toString();
        }
        throw (new IllegalArgumentException("Invalid Object in QueryValue: "
                    + value.toString()));
    }

    /**
     * Returns the FROM list for the SQL query.
     *
     * @return the SQL FROM list
     */
    protected String buildFromComponent() {
        String retval = "";
        boolean needComma = false;
        Set fromElements = query.getFrom();
        Iterator fromIter = fromElements.iterator();
        while (fromIter.hasNext()) {
            if (needComma) {
                retval += ", ";
            }
            needComma = true;
            Object fromElement = fromIter.next();
            if (fromElement instanceof QueryClass) {
                QueryClass qc = (QueryClass) fromElement;
                ClassDescriptor cld = dr.getDescriptorFor(qc.getType());
                String alias = (String) query.getAliases().get(qc);
                retval += cld.getFullTableName() + " AS " + alias;
            } else {
                Query q = (Query) fromElement;
                String alias = (String) query.getAliases().get(q);
                retval += "(" + (new FlymineSqlSelectStatement(q, dr, false)).getStatement()
                    + ") AS " + alias;
            }
        }
        return retval;
    }

    /**
     * Returns the WHERE clause for the SQL query.
     *
     * @return the SQL WHERE clause
     */
    protected String buildWhereClause() {
        // TODO:
        Constraint c = query.getConstraint();
        if (c != null) {
            return " WHERE " + constraintToString(c);
        }
        return "";
    }

    /**
     * Converts a Constraint object into a String suitable for putting in an SQL query.
     *
     * @param c the Constraint object
     * @return the converted String
     */
    protected String constraintToString(Constraint c) {
        if (c instanceof ConstraintSet) {
            ConstraintSet cs = (ConstraintSet) c;
            String retval = (cs.isNegated() ? "( NOT (" : "(");
            boolean needComma = false;
            Set constraints = cs.getConstraints();
            Iterator constraintIter = constraints.iterator();
            while (constraintIter.hasNext()) {
                Constraint subC = (Constraint) constraintIter.next();
                if (needComma) {
                    retval +=  (cs.getDisjunctive() ? " OR " : " AND ");
                }
                needComma = true;
                retval += constraintToString(subC);
            }
            return retval + (cs.isNegated() ? "))" : ")");
        } else if (c instanceof SimpleConstraint) {
            SimpleConstraint sc = (SimpleConstraint) c;
            if ((sc.getType() == SimpleConstraint.IS_NULL)
                    || (sc.getType() == SimpleConstraint.IS_NOT_NULL)) {
                return queryEvaluableToString(sc.getArg1()) + sc.getOpString();
            } else {
                return queryEvaluableToString(sc.getArg1()) + sc.getOpString()
                    + queryEvaluableToString(sc.getArg2());
            }
        } else if (c instanceof SubqueryConstraint) {
            SubqueryConstraint sc = (SubqueryConstraint) c;
            Query q = sc.getQuery();
            QueryEvaluable qe = sc.getQueryEvaluable();
            QueryClass cls = sc.getQueryClass();
            if (qe != null) {
                return queryEvaluableToString(qe) + (sc.isNotIn() ? " NOT IN (" : " IN (")
                    + (new FlymineSqlSelectStatement(q, dr, true)).getStatement() + ")";
            } else {
                return queryClassToString(cls, false, true) + (sc.isNotIn() ? " NOT IN (" : " IN (")
                    + (new FlymineSqlSelectStatement(q, dr, true)).getStatement() + ")";
            }
        } else if (c instanceof ClassConstraint) {
            ClassConstraint cc = (ClassConstraint) c;
            QueryClass arg1 = cc.getArg1();
            String alias1 = ((String) query.getAliases().get(arg1)) + ".";
            QueryClass arg2QC = cc.getArg2QueryClass();
            String alias2 = null;
            if (arg2QC != null) {
                alias2 = ((String) query.getAliases().get(arg2QC)) + ".";
            }
            Object arg2O = cc.getArg2Object();
            ClassDescriptor cld = dr.getDescriptorFor(arg1.getType());
            if (cld == null) {
                throw (new IllegalArgumentException("Couldn't find class descriptor for class "
                            + arg1.getType()));
            }
            FieldDescriptor fields[] = cld.getPkFields();
            String retval = (cc.isNotEqual() ? "( NOT (" : "(");
            boolean needComma = false;
            for (int i = 0; i < fields.length; i++) {
                FieldDescriptor field = fields[i];
                String columnname = field.getColumnName();
                if (needComma) {
                    retval += " AND ";
                }
                needComma = true;
                if (arg2QC != null) {
                    retval += alias1 + columnname + " = " + alias2 + columnname;
                } else {
                    retval += alias1 + columnname + " = "
                        + objectToString(field.getPersistentField().get(arg2O));
                }
            }
            return retval + (cc.isNotEqual() ? "))" : ")");
        }
        return "";
    }

    /**
     * Returns the GROUP BY clause for the SQL query.
     *
     * @return the SQL GROUP BY clause
     */
    protected String buildGroupBy() {
        String retval = "";
        boolean needComma = false;
        Set groupBy = query.getGroupBy();
        Iterator groupByIter = groupBy.iterator();
        while (groupByIter.hasNext()) {
            QueryNode node = (QueryNode) groupByIter.next();
            retval += (needComma ? ", " : " GROUP BY ");
            needComma = true;
            if (node instanceof QueryClass) {
                retval += queryClassToString((QueryClass) node, false, false);
            } else {
                retval += queryEvaluableToString((QueryEvaluable) node);
            }
        }
        return retval;
    }

    /**
     * Returns the ORDER BY clause for the SQL query.
     *
     * @return the SQL ORDER BY clause
     */
    protected String buildOrderBy() {
        String retval = "";
        boolean needComma = false;
        List orderBy = query.getOrderBy();
        Iterator orderByIter = orderBy.iterator();
        while (orderByIter.hasNext()) {
            QueryNode node = (QueryNode) orderByIter.next();
            retval += (needComma ? ", " : " ORDER BY ");
            needComma = true;
            if (node instanceof QueryClass) {
                retval += queryClassToString((QueryClass) node, false, true);
            } else {
                retval += queryEvaluableToString((QueryEvaluable) node);
            }
        }
        List select = query.getSelect();
        Iterator selectIter = select.iterator();
        while (selectIter.hasNext()) {
            QueryNode node = (QueryNode) selectIter.next();
            retval += (needComma ? ", " : " ORDER BY ");
            needComma = true;
            if (node instanceof QueryClass) {
                retval += queryClassToString((QueryClass) node, false, true);
            } else if (node instanceof QueryValue) {
                // Do nothing
                retval = retval;
            } else {
                retval += queryEvaluableToString((QueryEvaluable) node);
            }
        }
        return retval;
    }

    /**
     * Return the statement as a string
     *
     * @return sql statement as a string
     */
    public String getStatement() {
        return "SELECT " + (query.isDistinct() ? "DISTINCT " : "") + buildSelectComponent()
            + " FROM " + buildFromComponent()
            + buildWhereClause() + buildGroupBy()
            + (isSubQuery ? "" : buildOrderBy() + " LIMIT " + limit + " OFFSET " + start);
    }

}
