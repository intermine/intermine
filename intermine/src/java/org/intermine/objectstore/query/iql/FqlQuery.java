package org.flymine.objectstore.query.fql;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.flymine.util.Util;
import org.flymine.objectstore.query.*;

/**
 * OQL representation of an object-based Query
 *
 * @author Andrew Varley
 */
public class FqlQuery
{
    private String queryString;
    private String packageName;
    private List parameters = new ArrayList();

    /**
     * No-arg constructor (for deserialization)
     */
    public FqlQuery() {
    }

    /**
     * Construct an FQL query from a String.
     * NOTE: The query string is not validated on construction
     *
     * @param queryString the string-based query
     * @param packageName the package name with which to qualify unqualified classnames. Note that
     * this can be null if every class name is fully qualified
     * @throws NullPointerException if queryString is null
     */
    public FqlQuery(String queryString, String packageName) {
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
    }


    /**
     * Construct an FQL query from a Query object.
     *
     * @param q the Query object
     * @throws NullPointerException if query is null
     */
    public FqlQuery(Query q) {
        if (q == null) {
            throw new NullPointerException("query should not be null");
        }

        boolean needComma = false;
        String retval = "SELECT ";
        Iterator selectIter = q.getSelect().iterator();
        while (selectIter.hasNext()) {
            QueryNode qn = (QueryNode) selectIter.next();
            if (needComma) {
                retval += ", ";
            }
            needComma = true;
            String nodeAlias = (String) q.getAliases().get(qn);
            if (qn instanceof QueryClass) {
                retval += nodeToString(q, qn);
            } else {
                retval += nodeToString(q, qn) + (nodeAlias == null ? "" : " AS " + nodeAlias);
            }
        }
        needComma = false;
        retval += " FROM ";
        Iterator qcIter = q.getFrom().iterator();
        while (qcIter.hasNext()) {
            FromElement fe = (FromElement) qcIter.next();
            String classAlias = (String) q.getAliases().get(fe);
            if (needComma) {
                retval += ", ";
            }
            needComma = true;
            if (fe instanceof QueryClass) {
                retval += fe.toString() + (classAlias == null ? "" : " AS " + classAlias);
            } else {
                retval += "(" + fe.toString() + ")" + (classAlias == null ? "" : " AS "
                        + classAlias);
            }
        }
        retval += (q.getConstraint() == null ? "" : " WHERE "
                   + constraintToString(q, q.getConstraint(), parameters));
        if (!q.getGroupBy().isEmpty()) {
            retval += " GROUP BY ";
            Iterator groupIter = q.getGroupBy().iterator();
            needComma = false;
            while (groupIter.hasNext()) {
                QueryNode qn = (QueryNode) groupIter.next();
                if (needComma) {
                    retval += ", ";
                }
                needComma = true;
                retval += nodeToString(q, qn);
            }
        }
        if (!q.getOrderBy().isEmpty()) {
            retval += " ORDER BY ";
            Iterator orderIter = q.getOrderBy().iterator();
            needComma = false;
            while (orderIter.hasNext()) {
                QueryNode qn = (QueryNode) orderIter.next();
                if (needComma) {
                    retval += ", ";
                }
                needComma = true;
                retval += nodeToString(q, qn);
            }
        }
        queryString = retval;
    }

    /**
     * Converts a QueryNode into a String.
     *
     * @param q a Query, to provide aliases
     * @param qn a QueryNode to convert
     * @return a String
     */
    public static String nodeToString(Query q, QueryNode qn) {
        String nodeAlias = (String) q.getAliases().get(qn);
        if (qn instanceof QueryClass) {
            return nodeAlias;
        } else if (qn instanceof QueryField) {
            QueryField qf = (QueryField) qn;
            return q.getAliases().get(qf.getFromElement()) + "." + qf.getFieldName()
                + (qf.getSecondFieldName() == null ? "" : "." + qf.getSecondFieldName());
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
                    + ", " + nodeToString(q, qe.getArg3()) + ")";
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
                    default:
                        throw (new IllegalArgumentException("Invalid QueryFunction operation: "
                                    + qf.getOperation()));
                }
                retval += nodeToString(q, qf.getParam()) + ")";
                return retval;
            }
        } else {
            return qn.toString();
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
                return nodeToString(q, c.getArg1()) + " " + c.getOpString();
            } else {
                return nodeToString(q, c.getArg1()) + " " + c.getOpString()
                    + " " + nodeToString(q, c.getArg2());
            }
        } else if (cc instanceof SubqueryConstraint) {
            SubqueryConstraint c = (SubqueryConstraint) cc;
            FqlQuery subquery = new FqlQuery(c.getQuery());
            // Add the parameters of the subquery to this query
            parameters.addAll(subquery.getParameters());
            return (c.getQueryEvaluable() == null ? nodeToString(q, c.getQueryClass())
                    : nodeToString(q, c.getQueryEvaluable()))
                + (c.isNotIn() ? " NOT IN (" : " IN (")
                + subquery.getQueryString() + ")";
        } else if (cc instanceof ClassConstraint) {
            ClassConstraint c = (ClassConstraint) cc;
            String retval = nodeToString(q, c.getArg1()) + (c.isNotEqual() ? " != " : " = ");
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
            return q.getAliases().get(ref.getQueryClass()) + "." + ref.getFieldName()
                + (c.isNotContains() ? " DOES NOT CONTAIN " : " CONTAINS ")
                + q.getAliases().get(c.getQueryClass());
        } else if (cc instanceof ConstraintSet) {
            ConstraintSet c = (ConstraintSet) cc;
            boolean needComma = false;
            String retval = (c.isNegated() ? "( NOT (" : "(");
            Iterator conIter = c.getConstraints().iterator();
            while (conIter.hasNext()) {
                Constraint subC = (Constraint) conIter.next();
                if (needComma) {
                    retval += (c.getDisjunctive() ? " OR " : " AND ");
                }
                needComma = true;
                retval += constraintToString(q, subC, parameters);
            }
            return retval + (c.isNegated() ? "))" : ")");
        } else {
            throw new IllegalArgumentException("Unknown constraint type: " + cc);
        }
    }


    /**
     * Convert to a FlyMine query
     *
     * @return the FlyMine Query object
     */
    public Query toQuery() {
        return FqlQueryParser.parse(this);
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
     * Get the package name
     *
     * @return the package name
     */
    public String getPackageName() {
        return packageName;
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
     * Return a string version of the FqlQuery
     *
     * @return a String version of the query
     */
    public String toString() {
        StringBuffer ret = new StringBuffer(queryString);
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
        if (!(o instanceof FqlQuery)) {
            return false;
        }
        FqlQuery f = (FqlQuery) o;
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
}
