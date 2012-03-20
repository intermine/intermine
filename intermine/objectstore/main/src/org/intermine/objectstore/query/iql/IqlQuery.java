package org.intermine.objectstore.query.iql;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ClassConstraint;
import org.intermine.objectstore.query.Clob;
import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.FromElement;
import org.intermine.objectstore.query.MultipleInBagConstraint;
import org.intermine.objectstore.query.ObjectStoreBag;
import org.intermine.objectstore.query.ObjectStoreBagCombination;
import org.intermine.objectstore.query.ObjectStoreBagsForObject;
import org.intermine.objectstore.query.OrderDescending;
import org.intermine.objectstore.query.OverlapConstraint;
import org.intermine.objectstore.query.PathExpressionField;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryCast;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryClassBag;
import org.intermine.objectstore.query.QueryCollectionPathExpression;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryEvaluable;
import org.intermine.objectstore.query.QueryExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryForeignKey;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.QueryObjectPathExpression;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryOrderable;
import org.intermine.objectstore.query.QueryReference;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SubqueryConstraint;
import org.intermine.objectstore.query.SubqueryExistsConstraint;
import org.intermine.util.DynamicUtil;
import org.intermine.util.Util;

/**
 * OQL representation of an object-based Query
 *
 * @author Andrew Varley
 * @author Matthew Wakeling
 */
public class IqlQuery
{
    private String queryString;
    private String packageName;
    private List<?> parameters;

    /**
     * No-arg constructor (for deserialization)
     */
    public IqlQuery() {
    }

    /**
     * Construct an IQL query from a String.
     * NOTE: The query string is not validated on construction
     *
     * @param queryString the string-based query
     * @param packageName the package name with which to qualify unqualified classnames. Note that
     * this can be null if every class name is fully qualified
     * @param parameters values  to substitute for '?' in query text
     * @throws NullPointerException if queryString is null
     */
    public IqlQuery(String queryString, String packageName, List<?> parameters) {
        if (queryString == null) {
            throw new NullPointerException("queryString should not be null");
        }
        if ("".equals(queryString)) {
            throw new IllegalArgumentException("queryString should not be empty");
        }
        if ("".equals(packageName)) {
            throw new IllegalArgumentException("packageName should not be empty");
        }
        this.queryString = queryString;
        this.packageName = packageName;
        if (parameters != null) {
            this.parameters = parameters;
        } else {
            this.parameters = new ArrayList<Object>();
        }
    }

    /**
     * Construct an IQL query from a String.
     * NOTE: The query string is not validated on construction
     *
     * @param queryString the string-based query
     * @param packageName the package name with which to qualify unqualified classnames. Note that
     * this can be null if every class name is fully qualified
     * @throws NullPointerException if queryString is null
     */
    public IqlQuery(String queryString, String packageName) {
        this(queryString, packageName, null);
    }

    /**
     * Construct an IQL query from a Query object.
     *
     * @param q the Query object
     * @throws NullPointerException if query is null
     */
    public IqlQuery(Query q) {
        List<Object> newParameters = new ArrayList<Object>();
        if (q == null) {
            throw new NullPointerException("query should not be null");
        }

        boolean needComma = false;
        StringBuffer retval = new StringBuffer(q.isDistinct() ? "SELECT DISTINCT " : "SELECT ");
        Set<QueryObjectPathExpression> pathList = new HashSet<QueryObjectPathExpression>();
        for (QuerySelectable qn : q.getSelect()) {
            if (needComma) {
                retval.append(", ");
            }
            needComma = true;
            String nodeAlias = q.getAliases().get(qn);
            if ((qn instanceof QueryClass) || (qn instanceof ObjectStoreBag)
                    || (qn instanceof ObjectStoreBagCombination)
                    || (qn instanceof ObjectStoreBagsForObject)
                    || (qn instanceof Clob)) {
                retval.append(nodeToString(q, qn, newParameters, null));
            } else {
                retval.append(nodeToString(q, qn, newParameters, pathList))
                    .append(nodeAlias == null ? "" : " AS " + escapeReservedWord(nodeAlias));
            }
        }
        needComma = false;
        for (FromElement fe : q.getFrom()) {
            String classAlias = escapeReservedWord(q.getAliases().get(fe));
            retval.append(needComma ? ", " : " FROM ");
            needComma = true;
            if (fe instanceof QueryClass) {
                retval.append(fe.toString())
                    .append(classAlias == null ? "" : " AS " + classAlias);
            } else if (fe instanceof QueryClassBag) {
                retval.append(fe.toString())
                    .append(classAlias == null ? "" : " AS " + classAlias);
                Collection<?> coll = ((QueryClassBag) fe).getBag();
                if (coll != null) {
                    newParameters.add(coll);
                }
            } else {
                retval.append("(")
                    .append(fe.toString())
                    .append(")")
                    .append(classAlias == null ? "" : " AS " + classAlias);
            }
        }
        if (q.getConstraint() != null) {
            retval.append(" WHERE ")
                .append(constraintToString(q, q.getConstraint(), newParameters));
        }
        needComma = false;
        for (QueryNode qn : q.getGroupBy()) {
            retval.append(needComma ? ", " : " GROUP BY ");
            needComma = true;
            retval.append(nodeToString(q, qn, newParameters, null));
        }
        needComma = false;
        for (QueryOrderable qn : q.getOrderBy()) {
            retval.append(needComma ? ", " : " ORDER BY ");
            needComma = true;
            retval.append(nodeToString(q, qn, newParameters, null));
        }
        if (q.getLimit() != Integer.MAX_VALUE) {
            retval.append(" LIMIT " + q.getLimit());
        }
        needComma = false;
        for (QueryObjectPathExpression qope : pathList) {
            retval.append(needComma ? ", " : " PATH ");
            needComma = true;
            retval.append(nodeToString(q, qope, newParameters, null))
                .append(" AS ")
                .append(q.getAliases().get(qope));
        }
        parameters = newParameters;
        queryString = retval.toString();
    }

    /**
     * Converts an Object into a String.
     *
     * @param q a Query, to provide aliases
     * @param qn an Object to convert
     * @param parameters a List, in which this method will place objects corresponding to the
     * question marks in the resulting String
     * @param pathList QueryObjectPathExpressions will be added to this for PathExpressionField
     * objects in the SELECT list
     * @return a String
     */
    public static String nodeToString(Query q, Object qn, List<Object> parameters,
            Set<QueryObjectPathExpression> pathList) {
        if (qn instanceof QueryClass) {
            String nodeAlias = q.getAliases().get(qn);
            return escapeReservedWord(nodeAlias);
        } else if (qn instanceof QueryField) {
            QueryField qf = (QueryField) qn;
            return escapeReservedWord(q.getAliases().get(qf.getFromElement())) + "."
                + escapeReservedWord(qf.getFieldName()) + (qf.getSecondFieldName() == null ? ""
                        : "." + escapeReservedWord(qf.getSecondFieldName()));
        } else if (qn instanceof QueryValue) {
            Object obj = ((QueryValue) qn).getValue();
            if (obj instanceof String) {
                return "'" + obj + "'";
            } else if (obj instanceof Date) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                return "'" + format.format((Date) obj) + "'";
            } else if (obj instanceof Class) {
                return "'" + ((Class<?>) obj).getName() + "'";
            } else {
                return obj.toString();
            }
        } else if (qn instanceof QueryExpression) {
            return queryExpressionToString(q, (QueryExpression) qn, parameters);
        } else if (qn instanceof QueryFunction) {
            QueryFunction qf = (QueryFunction) qn;
            if (qf.getOperation() == QueryFunction.COUNT) {
                return "COUNT(*)";
            } else {
                String retval = null;
                switch (qf.getOperation()) {
                    case QueryFunction.SUM:
                        retval = "SUM(";
                        break;
                    case QueryFunction.AVERAGE:
                        retval = "AVG(";
                        break;
                    case QueryFunction.MIN:
                        retval = "MIN(";
                        break;
                    case QueryFunction.MAX:
                        retval = "MAX(";
                        break;
                    case QueryFunction.STDDEV:
                        retval = "STDDEV(";
                        break;
                    default:
                        throw (new IllegalArgumentException("Invalid QueryFunction operation: "
                                    + qf.getOperation()));
                }
                retval += nodeToString(q, qf.getParam(), parameters, null) + ")";
                return retval;
            }
        } else if (qn instanceof QueryCast) {
            QueryCast qc = (QueryCast) qn;
            String type = qc.getType().getName();
            return "(" + nodeToString(q, qc.getValue(), parameters, null) + ")::"
                + type.substring(type.lastIndexOf('.') + 1);
        } else if (qn instanceof QueryObjectReference) {
            QueryObjectReference ref = (QueryObjectReference) qn;
            return q.getAliases().get(ref.getQueryClass()) + "." + ref.getFieldName();
        } else if (qn instanceof QueryForeignKey) {
            QueryForeignKey key = (QueryForeignKey) qn;
            return q.getAliases().get(key.getQueryClass()) + "." + key.getFieldName() + ".id";
        } else if (qn instanceof QueryObjectPathExpression) {
            return queryObjectPathExpressionToString(q, parameters, (QueryObjectPathExpression) qn);
        } else if (qn instanceof QueryCollectionPathExpression) {
            return queryCollectionPathExpressionToString(q, parameters,
                    (QueryCollectionPathExpression) qn);
        } else if (qn instanceof PathExpressionField) {
            QueryObjectPathExpression qope = ((PathExpressionField) qn).getQope();
            pathList.add(qope);
            return q.getAliases().get(qope) + "." + ((PathExpressionField) qn).getFieldNumber();
        } else if (qn instanceof ObjectStoreBag) {
            return "BAG(" + ((ObjectStoreBag) qn).getBagId() + ")";
        } else if (qn instanceof ObjectStoreBagCombination) {
            StringBuffer retval = new StringBuffer();
            ObjectStoreBagCombination osbc = (ObjectStoreBagCombination) qn;
            boolean needComma = false;
            for (ObjectStoreBag osb : osbc.getBags()) {
                if (needComma) {
                    switch(osbc.getOp()) {
                        case ObjectStoreBagCombination.UNION:
                            retval.append(" UNION ");
                            break;
                        case ObjectStoreBagCombination.INTERSECT:
                            retval.append(" INTERSECT ");
                            break;
                        case ObjectStoreBagCombination.EXCEPT:
                            retval.append(" EXCEPT ");
                            break;
                        case ObjectStoreBagCombination.ALLBUTINTERSECT:
                            retval.append(" ALLBUTINTERSECT ");
                            break;
                        default:
                            throw new IllegalStateException("Illegal op: " + osbc.getOp());
                    }
                }
                needComma = true;
                retval.append(nodeToString(q, osb, parameters, null));
            }
            return retval.toString();
        } else if (qn instanceof ObjectStoreBagsForObject) {
            ObjectStoreBagsForObject osbfo = (ObjectStoreBagsForObject) qn;
            StringBuffer retval = new StringBuffer("BAGS FOR " + osbfo.getValue());
            Collection<ObjectStoreBag> bags = osbfo.getBags();
            if (bags != null) {
                retval.append(" IN BAGS ?");
                parameters.add(bags);
            }
            return retval.toString();
        } else if (qn instanceof Clob) {
            return "CLOB(" + ((Clob) qn).getClobId() + ")";
        } else if (qn instanceof OrderDescending) {
            return nodeToString(q, ((OrderDescending) qn).getQueryOrderable(), parameters, null)
                + " DESC";
        } else {
            throw new IllegalArgumentException("Invalid Object for nodeToString: " + qn);
        }
    }

    /**
     * Converts a QueryObjectPathExpression into a String.
     *
     * @param q a Query, to provide aliases
     * @param parameters a List, in which this method will place objects corresponding to the
     * question marks in the resulting String
     * @param col an Object to convert
     * @return a String
     */
    public static String queryCollectionPathExpressionToString(Query q, List<Object> parameters,
            QueryCollectionPathExpression col) {
        StringBuffer retval = new StringBuffer();
        retval.append(q.getAliases().get(col.getQueryClass()))
            .append(".")
            .append(col.getFieldName());
        if (col.getSubclass() != null) {
            Class<?> subclass = col.getSubclass();
            Collection<Class<?>> subclasses = DynamicUtil.decomposeClass(subclass);
            if (subclasses.size() == 1) {
                retval.append("::")
                    .append(subclasses.iterator().next().getName());
            } else {
                boolean needComma = false;
                for (Class<?> subclas : subclasses) {
                    retval.append(needComma ? ", " : "::(");
                    needComma = true;
                    retval.append(subclas.getName());
                }
                retval.append(")");
            }
        }
        if ((!col.getSelect().isEmpty()) || (!col.getFrom().isEmpty())
                || (col.getConstraint() != null)) {
            Set<InterMineObject> empty = Collections.emptySet();
            Query subQ = col.getQuery(empty);
            retval.append("(");
            boolean needSpace = false;
            Set<QueryObjectPathExpression> pathList2 = new HashSet<QueryObjectPathExpression>();
            if (!col.getSelect().isEmpty()) {
                retval.append("SELECT ");
                if (col.isSingleton()) {
                    retval.append("SINGLETON ");
                }
                boolean needComma = false;
                for (QuerySelectable selectable : col.getSelect()) {
                    if (needComma) {
                        retval.append(", ");
                    }
                    needComma = true;
                    retval.append(nodeToString(subQ, selectable, parameters, pathList2));
                }
                needSpace = true;
            }
            if (!col.getFrom().isEmpty()) {
                if (needSpace) {
                    retval.append(" ");
                }
                retval.append("FROM ");
                boolean needComma = false;
                for (FromElement node : col.getFrom()) {
                    if (needComma) {
                        retval.append(", ");
                    }
                    needComma = true;
                    String classAlias = escapeReservedWord(subQ.getAliases().get(node));
                    if (node instanceof QueryClass) {
                        retval.append(node.toString())
                            .append(classAlias == null ? "" : " AS " + classAlias);
                    } else if (node instanceof QueryClassBag) {
                        retval.append(node.toString())
                            .append(classAlias == null ? "" : " AS " + classAlias);
                    } else {
                        retval.append("(")
                            .append(node.toString())
                            .append(")")
                            .append(classAlias == null ? "" : " AS " + classAlias);
                    }
                }
                needSpace = true;
            }
            if (col.getConstraint() != null) {
                if (needSpace) {
                    retval.append(" ");
                }
                retval.append("WHERE ")
                    .append(constraintToString(subQ, col.getConstraint(), parameters));
            }
            boolean needComma = false;
            for (QueryObjectPathExpression qope : pathList2) {
                retval.append(needComma ? ", " : " PATH ");
                needComma = true;
                retval.append(nodeToString(subQ, qope, parameters, null))
                    .append(" AS ")
                    .append(subQ.getAliases().get(qope));
            }
            retval.append(")");
        }
        return retval.toString();
    }

    /**
     * Converts a QueryObjectPathExpression into a String.
     *
     * @param q a Query, to provide aliases
     * @param parameters a List, in which this method will place objects corresponding to the
     * question marks in the resulting String
     * @param ref an Object to convert
     * @return a String
     */
    public static String queryObjectPathExpressionToString(Query q, List<Object> parameters,
            QueryObjectPathExpression ref) {
        StringBuffer retval = new StringBuffer();
        retval.append(q.getAliases().get(ref.getQueryClass()))
            .append(".")
            .append(ref.getFieldName());
        if (ref.getSubclass() != null) {
            Class<?> subclass = ref.getSubclass();
            Collection<Class<?>> subclasses = DynamicUtil.decomposeClass(subclass);
            if (subclasses.size() == 1) {
                retval.append("::")
                    .append(subclasses.iterator().next().getName());
            } else {
                boolean needComma = false;
                for (Class<?> subclas : subclasses) {
                    retval.append(needComma ? ", " : "::(");
                    needComma = true;
                    retval.append(subclas.getName());
                }
                retval.append(")");
            }
        }
        if ((!ref.getSelect().isEmpty()) || (ref.getConstraint() != null)) {
            Set<Integer> empty = Collections.emptySet();
            Query subQ = ref.getQuery(empty, true);
            retval.append("(");
            boolean needSpace = false;
            Set<QueryObjectPathExpression> subPathList
                = new HashSet<QueryObjectPathExpression>();
            if (!ref.getSelect().isEmpty()) {
                retval.append("SELECT ");
                boolean needComma = false;
                for (QuerySelectable selectable : ref.getSelect()) {
                    if (needComma) {
                        retval.append(", ");
                    }
                    needComma = true;
                    retval.append(nodeToString(subQ, selectable, parameters, subPathList));
                }
                needSpace = true;
            }
            if (ref.getConstraint() != null) {
                if (needSpace) {
                    retval.append(" ");
                }
                retval.append("WHERE ")
                    .append(constraintToString(subQ, ref.getConstraint(), parameters));
            }
            boolean needComma = false;
            for (QueryObjectPathExpression qope : subPathList) {
                retval.append(needComma ? ", " : " PATH ");
                needComma = true;
                retval.append(nodeToString(subQ, qope, parameters, null))
                    .append(" AS ")
                    .append(subQ.getAliases().get(qope));
            }
            retval.append(")");
        }
        return retval.toString();
    }

    /**
     * Converts a QueryExpression into a String.
     *
     * @param q a Query, to provide aliases
     * @param qe an Object to convert
     * @param parameters a List, in which this method will place objects corresponding to the
     * question marks in the resulting String
     * @return a String
     */
    public static String queryExpressionToString(Query q, QueryExpression qe,
            List<Object> parameters) {
        if (qe.getOperation() == QueryExpression.SUBSTRING) {
            return "SUBSTR(" + nodeToString(q, qe.getArg1(), parameters, null) + ", "
                + nodeToString(q, qe.getArg2(), parameters, null)
                + (qe.getArg3() == null ? "" : ", "
                        + nodeToString(q, qe.getArg3(), parameters, null)) + ")";
        } else if (qe.getOperation() == QueryExpression.INDEX_OF) {
            return "INDEXOF(" + nodeToString(q, qe.getArg1(), parameters, null) + ", "
                + nodeToString(q, qe.getArg2(), parameters, null) + ")";
        } else if (qe.getOperation() == QueryExpression.UPPER) {
            return "UPPER(" + nodeToString(q, qe.getArg1(), parameters, null) + ")";
        } else if (qe.getOperation() == QueryExpression.LOWER) {
            return "LOWER(" + nodeToString(q, qe.getArg1(), parameters, null) + ")";
        } else if (qe.getOperation() == QueryExpression.GREATEST) {
            return "GREATEST(" + nodeToString(q, qe.getArg1(), parameters, null) + ","
                + nodeToString(q, qe.getArg2(), parameters, null) + ")";
        } else if (qe.getOperation() == QueryExpression.LEAST) {
            return "LEAST(" + nodeToString(q, qe.getArg1(), parameters, null) + ","
                + nodeToString(q, qe.getArg2(), parameters, null) + ")";
        } else {
            String retval = nodeToString(q, qe.getArg1(), parameters, null);
            switch (qe.getOperation()) {
                case QueryExpression.ADD:
                    retval += " + ";
                    break;
                case QueryExpression.SUBTRACT:
                    retval += " - ";
                    break;
                case QueryExpression.MULTIPLY:
                    retval += " * ";
                    break;
                case QueryExpression.DIVIDE:
                    retval += " / ";
                    break;
                default:
                    throw (new IllegalArgumentException("Invalid QueryExpression operation: "
                                + qe.getOperation()));
            }
            retval += nodeToString(q, qe.getArg2(), parameters, null);
            return retval;
        }
    }

    /**
     * Converts a Constraint into a String.
     *
     * @param q a Query, to provide aliases
     * @param cc a Constraint to convert
     * @param parameters a List, in which this method will place objects corresponding to the
     * question marks in the resulting String
     * @return a String
     */
    public static String constraintToString(Query q, Constraint cc, List<Object> parameters) {
        if (cc instanceof SimpleConstraint) {
            SimpleConstraint c = (SimpleConstraint) cc;
            if (c.getArg2() == null) {
                return nodeToString(q, c.getArg1(), parameters, null) + " " + c.getOp().toString();
            } else {
                return nodeToString(q, c.getArg1(), parameters, null) + " " + c.getOp().toString()
                    + " " + nodeToString(q, c.getArg2(), parameters, null);
            }
        } else if (cc instanceof SubqueryConstraint) {
            SubqueryConstraint c = (SubqueryConstraint) cc;
            IqlQuery subquery = c.getQuery().getIqlQuery();
            // Add the parameters of the subquery to this query
            parameters.addAll(subquery.getParameters());
            return (c.getQueryEvaluable() == null ? nodeToString(q, c.getQueryClass(), parameters,
                        null) : nodeToString(q, c.getQueryEvaluable(), parameters, null))
                + " " + c.getOp().toString() + " ("
                + subquery.getQueryString() + ")";
        } else if (cc instanceof ClassConstraint) {
            ClassConstraint c = (ClassConstraint) cc;
            String retval = nodeToString(q, c.getArg1(), parameters, null) + " "
                + c.getOp().toString() + " ";
            if (c.getArg2QueryClass() == null) {
                // Have an example object
                retval += "?";
                parameters.add(c.getArg2Object());
            } else {
                retval += nodeToString(q, c.getArg2QueryClass(), parameters, null);
            }
            return retval;
        } else if (cc instanceof ContainsConstraint) {
            ContainsConstraint c = (ContainsConstraint) cc;
            QueryReference ref = c.getReference();
            ConstraintOp op = c.getOp();
            String refString = queryReferenceToString(q, ref, parameters);
            if (op.equals(ConstraintOp.IS_NULL) || op.equals(ConstraintOp.IS_NOT_NULL)) {
                return refString + "." + ref.getFieldName() + " " + op.toString();
            } else if (c.getQueryClass() == null) {
                parameters.add(c.getObject());
                return refString + "." + ref.getFieldName() + " " + op.toString() + " ?";
            } else {
                return refString + "." + ref.getFieldName() + " " + op.toString() + " "
                    + q.getAliases().get(c.getQueryClass());
            }
        } else if (cc instanceof ConstraintSet) {
            ConstraintSet c = (ConstraintSet) cc;
            ConstraintOp op = c.getOp();
            boolean negate = (op == ConstraintOp.NAND) || (op == ConstraintOp.NOR);
            boolean disjunctive = (op == ConstraintOp.OR) || (op == ConstraintOp.NOR);
            if (!c.getConstraints().isEmpty()) {
                boolean needComma = false;
                String retval = (negate ? "( NOT (" : "(");
                for (Constraint subC : c.getConstraints()) {
                    if (needComma) {
                        retval += (disjunctive ? " OR " : " AND ");
                    }
                    needComma = true;
                    retval += constraintToString(q, subC, parameters);
                }
                return retval + (negate ? "))" : ")");
            }
            return (disjunctive == negate ? "true" : "false");
        } else if (cc instanceof BagConstraint) {
            BagConstraint c = (BagConstraint) cc;
            Collection<?> coll = c.getBag();
            if (coll == null) {
                return nodeToString(q, c.getQueryNode(), parameters, null) + " "
                    + c.getOp().toString() + " BAG(" + c.getOsb().getBagId() + ")";
            }
            parameters.add(coll);
            return nodeToString(q, c.getQueryNode(), parameters, null) + " " + c.getOp().toString()
                + " ?";
        } else if (cc instanceof MultipleInBagConstraint) {
            MultipleInBagConstraint c = (MultipleInBagConstraint) cc;
            Collection<?> coll = c.getBag();
            parameters.add(coll);
            StringBuilder retval = new StringBuilder("(");
            boolean needComma = false;
            for (QueryEvaluable qe : c.getEvaluables()) {
                if (needComma) {
                    retval.append(", ");
                }
                needComma = true;
                retval.append(nodeToString(q, qe, parameters, null));
            }
            retval.append(") IN ?");
            return retval.toString();
        } else if (cc instanceof SubqueryExistsConstraint) {
            IqlQuery subquery = ((SubqueryExistsConstraint) cc).getQuery().getIqlQuery();
            parameters.addAll(subquery.getParameters());
            return (cc.getOp().equals(ConstraintOp.EXISTS) ? "EXISTS (" : "DOES NOT EXIST (")
                + subquery.getQueryString() + ")";
        } else if (cc instanceof OverlapConstraint) {
            OverlapConstraint oc = (OverlapConstraint) cc;
            return "RANGE(" + nodeToString(q, oc.getLeft().getStart(), parameters, null) + ", "
                + nodeToString(q, oc.getLeft().getEnd(), parameters, null) + ", "
                + nodeToString(q, oc.getLeft().getParent(), parameters, null) + ") OVERLAPS RANGE("
                + nodeToString(q, oc.getRight().getStart(), parameters, null) + ", "
                + nodeToString(q, oc.getRight().getEnd(), parameters, null) + ", "
                + nodeToString(q, oc.getRight().getParent(), parameters, null) + ")";
        } else {
            throw new IllegalArgumentException("Unknown constraint type: " + cc);
        }
    }

    /**
     * Converts a QueryReference into a String.
     *
     * @param q a Query, from which to get aliases
     * @param ref a QueryReference
     * @param parameters a List to which parameters will be added
     * @return a String
     */
    public static String queryReferenceToString(Query q, QueryReference ref,
            List<Object> parameters) {
        if (ref.getQueryClass() != null) {
            return q.getAliases().get(ref.getQueryClass());
        } else if (((QueryCollectionReference) ref).getQcb() != null) {
            return q.getAliases().get(((QueryCollectionReference) ref).getQcb());
        } else {
            Object param = ((QueryCollectionReference) ref).getQcObject();
            if (param == null) {
                param = ((QueryCollectionReference) ref).getQcb();
            }
            parameters.add(param);
            return "?";
        }
    }

    /**
     * Convert to a InterMine query
     *
     * @return the InterMine Query object
     */
    public Query toQuery() {
        return IqlQueryParser.parse(this);
    }

    /**
     * Get the query String
     * NOTE: this will be unvalidated
     *
     * @return the query String
     */
    public String getQueryString() {
        return queryString;
    }

    /**
     * Set the query String
     * NOTE: this method is merely here to make this a Bean.
     *
     * @param queryString the query String
     */
    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    /**
     * Get the package name
     *
     * @return the package name
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Set the package name
     * NOTE: this method is merely here to make this a Bean.
     *
     * @param packageName the package name
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
     * Get the parameters
     *
     * @return the parameters
     */
    public List<?> getParameters() {
        return parameters;
    }

    /**
     * Set the parameters
     *
     * @param parameters the parameters
     */
    public void setParameters(List<?> parameters) {
        this.parameters = parameters;
    }

    /**
     * Return a string version of the IqlQuery
     *
     * @return a String version of the query
     */
    @Override
    public String toString() {
        StringBuffer ret = new StringBuffer();
        ret.append(queryString);
        int i = 0;
        for (Object o : parameters) {
            ret.append(" ")
                .append(++i)
                .append(": ")
                .append(o.toString());
        }
        return ret.toString();
    }

    /**
     * Return a string version of the IqlQuery but truncated the parameters (lists of bag contents)
     * to the specified length.
     *
     * @param maxSize the maximum length
     * @return a String version of the query
     */
    public String toStringTruncateParameters(int maxSize) {
        StringBuffer ret = new StringBuffer();
        ret.append(queryString);
        int i = 0;
        for (Object o : parameters) {
            if (Collection.class.isAssignableFrom(o.getClass())) {
                Collection<Object> col = (Collection<Object>) o;
                if (col.size() > maxSize) {
                    ArrayList<Object> truncated = new ArrayList<Object>();
                    int counter = 0;
                    for (Object element : col) {
                        if (counter >= maxSize) {
                            break;
                        }
                        truncated.add(element);
                        counter++;
                    }
                    o = truncated.toString() + " (showing " + maxSize + " of " + col.size() + ")";
                }
            }
            ret.append(" ")
                .append(++i)
                .append(": ")
                .append(o.toString());
        }
        return ret.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IqlQuery)) {
            return false;
        }
        IqlQuery f = (IqlQuery) o;
        return f.queryString.equals(queryString)
            && Util.equals(f.packageName, packageName)
            && Util.equals(f.parameters, parameters);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return 2 * queryString.hashCode()
            + 3 * Util.hashCode(packageName)
            + 5 * Util.hashCode(parameters);
    }

    private static final String[] RESERVED_WORDS = new String[] {
        "EXPLAIN",
        "SELECT",
        "ALL",
        "DISTINCT",
        "FROM",
        "WHERE",
        "GROUP",
        "BY",
        "ORDER",
        "AS",
        "TRUE",
        "FALSE",
        "OR",
        "AND",
        "NOT",
        "IN",
        "CONTAINS",
        "DOES",
        "CONTAIN",
        "LIKE",
        "IS",
        "COUNT",
        "MAX",
        "MIN",
        "SUM",
        "AVG",
        "SUBSTR",
        "INDEXOF"};
    private static Set<String> reservedWords = new HashSet<String>();
    static {
        for (int i = 0; i < RESERVED_WORDS.length; i++) {
            reservedWords.add(RESERVED_WORDS[i]);
        }
    }

    /**
     * Returns true if the given String is an IQL reserved word.
     *
     * @param word the String
     * @return a boolean
     */
    public static boolean isReservedWord(String word) {
        if (word != null) {
            return reservedWords.contains(word.toUpperCase());
        }
        return false;
    }

    /**
     * Converts words into escaped form.
     *
     * @param word the String
     * @return an escaped String
     */
    public static String escapeReservedWord(String word) {
        if (word != null) {
            if (isReservedWord(word) || (word.charAt(0) == '"')
                    || (word.charAt(word.length() - 1) == '"')) {
                return "\"" + word + "\"";
            }
        }
        return word;
    }
}
