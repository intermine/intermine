package org.intermine.objectstore.query.iql;

/*
 * Copyright (C) 2002-2005 FlyMine
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
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.intermine.util.Util;
import org.intermine.objectstore.query.*;

/**
 * OQL representation of an object-based Query
 *
 * @author Andrew Varley
 */
public class IqlQuery
{
    private String queryString;
    private String packageName;
    private List parameters = new ArrayList();

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
    public IqlQuery(String queryString, String packageName, List parameters) {
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
        if (q == null) {
            throw new NullPointerException("query should not be null");
        }

        boolean needComma = false;
        StringBuffer retval = new StringBuffer(q.isDistinct() ? "SELECT DISTINCT " : "SELECT ");
        Iterator selectIter = q.getSelect().iterator();
        while (selectIter.hasNext()) {
            QuerySelectable qn = (QuerySelectable) selectIter.next();
            if (needComma) {
                retval.append(", ");
            }
            needComma = true;
            String nodeAlias = (String) q.getAliases().get(qn);
            if ((qn instanceof QueryClass) || (qn instanceof ObjectStoreBag)) {
                retval.append(nodeToString(q, qn));
            } else {
                retval.append(nodeToString(q, qn))
                    .append(nodeAlias == null ? "" : " AS " + escapeReservedWord(nodeAlias));
            }
        }
        needComma = false;
        Iterator qcIter = q.getFrom().iterator();
        while (qcIter.hasNext()) {
            FromElement fe = (FromElement) qcIter.next();
            String classAlias = escapeReservedWord((String) q.getAliases().get(fe));
            retval.append(needComma ? ", " : " FROM ");
            needComma = true;
            if (fe instanceof QueryClass) {
                retval.append(fe.toString())
                    .append(classAlias == null ? "" : " AS " + classAlias);
            } else if (fe instanceof QueryClassBag) {
                retval.append(fe.toString())
                    .append(classAlias == null ? "" : " AS " + classAlias);
                Collection coll = ((QueryClassBag) fe).getBag();
                if (coll != null) {
                    parameters.add(coll);
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
                .append(constraintToString(q, q.getConstraint(), parameters));
        }
        Iterator groupIter = q.getGroupBy().iterator();
        needComma = false;
        while (groupIter.hasNext()) {
            QueryNode qn = (QueryNode) groupIter.next();
            retval.append(needComma ? ", " : " GROUP BY ");
            needComma = true;
            retval.append(nodeToString(q, qn));
        }
        Iterator orderIter = q.getOrderBy().iterator();
        needComma = false;
        while (orderIter.hasNext()) {
            QueryOrderable qn = (QueryOrderable) orderIter.next();
            retval.append(needComma ? ", " : " ORDER BY ");
            needComma = true;
            retval.append(nodeToString(q, qn));
        }
        queryString = retval.toString();
    }

    /**
     * Converts an Object into a String.
     *
     * @param q a Query, to provide aliases
     * @param qn an Object to convert
     * @return a String
     */
    public static String nodeToString(Query q, Object qn) {
        if (qn instanceof QueryClass) {
            String nodeAlias = (String) q.getAliases().get(qn);
            return escapeReservedWord(nodeAlias);
        } else if (qn instanceof QueryField) {
            QueryField qf = (QueryField) qn;
            return escapeReservedWord((String) (q.getAliases().get(qf.getFromElement()))) + "."
                + escapeReservedWord(qf.getFieldName()) + (qf.getSecondFieldName() == null ? ""
                        : "." + escapeReservedWord(qf.getSecondFieldName()));
        } else if (qn instanceof QueryValue) {
            Object obj = ((QueryValue) qn).getValue();
            if (obj instanceof String) {
                return "'" + obj + "'";
            } else if (obj instanceof Date) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                return "'" + format.format((Date) obj) + "'";
            } else {
                return obj.toString();
            }
        } else if (qn instanceof QueryExpression) {
            QueryExpression qe = (QueryExpression) qn;
            if (qe.getOperation() == QueryExpression.SUBSTRING) {
                return "SUBSTR(" + nodeToString(q, qe.getArg1()) + ", "
                    + nodeToString(q, qe.getArg2())
                    + (qe.getArg3() == null ? "" : ", " + nodeToString(q, qe.getArg3())) + ")";
            } else if (qe.getOperation() == QueryExpression.INDEX_OF) {
                return "INDEXOF(" + nodeToString(q, qe.getArg1()) + ", "
                    + nodeToString(q, qe.getArg2()) + ")";
            } else if (qe.getOperation() == QueryExpression.UPPER) {
                return "UPPER(" + nodeToString(q, qe.getArg1()) + ")";
            } else if (qe.getOperation() == QueryExpression.LOWER) {
                return "LOWER(" + nodeToString(q, qe.getArg1()) + ")";
            } else {
                String retval = nodeToString(q, qe.getArg1());
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
                retval += nodeToString(q, qe.getArg2());
                return retval;
            }
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
                retval += nodeToString(q, qf.getParam()) + ")";
                return retval;
            }
        } else if (qn instanceof QueryCast) {
            QueryCast qc = (QueryCast) qn;
            String type = qc.getType().getName();
            return "(" + nodeToString(q, qc.getValue()) + ")::"
                + type.substring(type.lastIndexOf('.') + 1);
        } else if (qn instanceof QueryObjectReference) {
            QueryObjectReference ref = (QueryObjectReference) qn;
            return q.getAliases().get(ref.getQueryClass()) + "." + ref.getFieldName();
        } else if (qn instanceof QueryObjectPathExpression) {
            QueryObjectPathExpression ref = (QueryObjectPathExpression) qn;
            return q.getAliases().get(ref.getQueryClass()) + "." + ref.getFieldName();
        } else if (qn instanceof QueryFieldPathExpression) {
            QueryFieldPathExpression ref = (QueryFieldPathExpression) qn;
            String objRepresentation = "null";
            if (ref.getDefaultValue() instanceof String) {
                objRepresentation = "'" + ref.getDefaultValue() + "'";
            } else if (ref.getDefaultValue() instanceof Date) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                objRepresentation = "'" + format.format((Date) ref.getDefaultValue()) + "'";
            } else if (ref.getDefaultValue() != null) {
                objRepresentation = ref.getDefaultValue().toString();
            }
            return q.getAliases().get(ref.getQueryClass()) + "." + ref.getReferenceName() + "."
                + ref.getFieldName() + "(DEF " + objRepresentation + ")";
        } else if (qn instanceof ObjectStoreBag) {
            return "BAG(" + ((ObjectStoreBag) qn).getBagId() + ")";
        } else if (qn instanceof OrderDescending) {
            return nodeToString(q, ((OrderDescending) qn).getQueryOrderable()) + " DESC";
        } else {
            throw new IllegalArgumentException("Invalid Object for nodeToString: " + qn.toString());
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
    public static String constraintToString(Query q, Constraint cc, List parameters) {
        if (cc instanceof SimpleConstraint) {
            SimpleConstraint c = (SimpleConstraint) cc;
            if (c.getArg2() == null) {
                return nodeToString(q, c.getArg1()) + " " + c.getOp().toString();
            } else {
                return nodeToString(q, c.getArg1()) + " " + c.getOp().toString()
                    + " " + nodeToString(q, c.getArg2());
            }
        } else if (cc instanceof SubqueryConstraint) {
            SubqueryConstraint c = (SubqueryConstraint) cc;
            IqlQuery subquery = new IqlQuery(c.getQuery());
            // Add the parameters of the subquery to this query
            parameters.addAll(subquery.getParameters());
            return (c.getQueryEvaluable() == null ? nodeToString(q, c.getQueryClass())
                    : nodeToString(q, c.getQueryEvaluable()))
                + " " + c.getOp().toString() + " ("
                + subquery.getQueryString() + ")";
        } else if (cc instanceof ClassConstraint) {
            ClassConstraint c = (ClassConstraint) cc;
            String retval = nodeToString(q, c.getArg1()) + " " + c.getOp().toString() + " ";
            if (c.getArg2QueryClass() == null) {
                // Have an example object
                retval += "?";
                parameters.add(c.getArg2Object());
            } else {
                retval += nodeToString(q, c.getArg2QueryClass());
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
                Iterator conIter = c.getConstraints().iterator();
                while (conIter.hasNext()) {
                    Constraint subC = (Constraint) conIter.next();
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
            Collection coll = c.getBag();
            if (coll == null) {
                return nodeToString(q, c.getQueryNode()) + " " + c.getOp().toString() + " BAG("
                    + c.getOsb().getBagId() + ")";
            }
            parameters.add(coll);
            return nodeToString(q, c.getQueryNode()) + " " + c.getOp().toString() + " ?";
        } else if (cc instanceof SubqueryExistsConstraint) {
            IqlQuery subquery = new IqlQuery(((SubqueryExistsConstraint) cc).getQuery());
            parameters.addAll(subquery.getParameters());
            return (cc.getOp().equals(ConstraintOp.EXISTS) ? "EXISTS (" : "DOES NOT EXIST (")
                + subquery.getQueryString() + ")";
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
    public static String queryReferenceToString(Query q, QueryReference ref, List parameters) {
        if (ref.getQueryClass() != null) {
            return (String) q.getAliases().get(ref.getQueryClass());
        } else if (((QueryCollectionReference) ref).getQcb() != null) {
            return (String) q.getAliases().get(((QueryCollectionReference) ref).getQcb());
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
    public List getParameters() {
        return parameters;
    }

    /**
     * Set the parameters
     *
     * @param parameters the parameters
     */
    public void setParameters(List parameters) {
        this.parameters = parameters;
    }

    /**
     * Return a string version of the IqlQuery
     *
     * @return a String version of the query
     */
    public String toString() {
        StringBuffer ret = new StringBuffer();
        ret.append(queryString);
        Iterator iter = parameters.iterator();
        int i = 0;
        while (iter.hasNext()) {
            ret.append(" ")
                .append(++i)
                .append(": ")
                .append(iter.next().toString());
        }
        return ret.toString();
    }

    /**
     * @see Object#equals
     */
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
     * @see Object#hashCode
     */
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
    private static Set reservedWords = new HashSet();
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
