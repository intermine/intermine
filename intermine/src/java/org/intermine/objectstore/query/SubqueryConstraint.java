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

import java.util.List;
import java.util.Arrays;

import org.flymine.util.Util;

/**
 * Constrain a QueryClass or QueryEvaluable to be within the select list
 * of a given subquery.
 *
 * @author Richard Smith
 * @author Mark Woodbridge
 */
public class SubqueryConstraint extends Constraint
{
    protected Query subquery;
    protected QueryOp type;
    protected QueryEvaluable qe;
    protected QueryClass cls;    

    /**
     * Construct a SubqueryConstraint with a QueryEvaluable
     *
     * @param query the subquery in question
     * @param type required type of constraint
     * @param qe item to match against subquery select
     */
    public SubqueryConstraint(Query query, QueryOp type, QueryEvaluable qe) {
        this(query, type, qe, false);
    }

    /**
     * Construct a SubqueryConstraint with a QueryEvaluable
     *
     * @param query the subquery in question
     * @param type required type of constraint
     * @param qe item to match against subquery select
     * @param negated reverse the constraint logic if true
     */
    public SubqueryConstraint(Query query, QueryOp type, QueryEvaluable qe, boolean negated) {

        if (query == null) {
            throw new NullPointerException("query cannot be null");
        }
  
        if (type == null) {
            throw new NullPointerException("type cannot be null");
        }

        if (!validOps().contains(type)) {
            throw new NullPointerException("type cannot be " + type);
        }

        if (qe == null) {
            throw new NullPointerException("qe cannot be null");
        }

        // check that query only has one item in select list
        List select = query.getSelect();
        if (select.size() < 1) {
            throw new IllegalArgumentException("Query has no items in select list.");
        }

        if (select.size() > 1) {
            throw new IllegalArgumentException("Subquery must have only one select item.");
        }

        // check that the select node is a QueryEvaluable
        QueryNode selectNode = (QueryNode) select.get(0);
        if (!QueryEvaluable.class.isAssignableFrom(selectNode.getClass())) {
            throw new IllegalArgumentException("Subquery select item is not a QueryEvaluable");
        }

        // check that java types of QueryEvaluables are comparable
        if (!(selectNode.getType().isAssignableFrom(qe.getType())
              || qe.getType().isAssignableFrom(selectNode.getType()))) {
            throw new IllegalArgumentException("Type of select from subquery ("
                                                + selectNode.getType()
                                                + ") not comparable with type from argument ("
                                                + qe.getType() + ")");
        }

        this.subquery = query;
        this.type = type;
        this.qe = qe;
        this.negated = negated;
    }

    /**
     * Construct a SubqueryConstraint with a QueryClass
     *
     * @param query the subquery in question
     * @param type required type of constraint
     * @param cls item to match against subquery select
     */
    public SubqueryConstraint(Query query, QueryOp type, QueryClass cls) {
        this(query, type, cls, false);
    }

    /**
     * Construct a SubqueryConstraint with a QueryClass
     *
     * @param query the subquery in question
     * @param type required type of constraint
     * @param cls item to match against subquery select
     * @param negated reverse the constraint logic if true
     */
    public SubqueryConstraint(Query query, QueryOp type, QueryClass cls, boolean negated) {
        if (query == null) {
            throw new NullPointerException("query cannot be null");
        }

        if (type == null) {
            throw new NullPointerException("type cannot be null");
        }

        if (!validOps().contains(type)) {
            throw new NullPointerException("type cannot be " + type);
        }

        if (cls == null) {
            throw new NullPointerException("cls cannot be null");
        }

        // check that query only has one item in select list and it is a QueryClass
        List select = query.getSelect();
        if (select.size() < 1) {
            throw new IllegalArgumentException("Query has no items in select list.");
        }

        if (select.size() > 1) {
            throw new IllegalArgumentException("Subquery must have only one "
                                                + "item in select list.");
        }

        QueryNode selectNode = (QueryNode) select.get(0);
        if (!QueryClass.class.isAssignableFrom(selectNode.getClass())) {
            throw new IllegalArgumentException("Select item of subquery is not a QueryClass");
        }

        if (!(selectNode.getType().isAssignableFrom(cls.getType())
              || cls.getType().isAssignableFrom(selectNode.getType()))) {
            throw new IllegalArgumentException("QueryClass select from subquery ("
                                                + selectNode.getType()
                                                + ") not comparable with QueryClass ("
                                                + cls.getType() + ")");
        }

        this.subquery = query;
        this.type = type;
        this.cls = cls;
        this.negated = negated;
    }

    /**
     * Get type of operation (i.e. contains or does not contain)
     *
     * @return type of operation
     */
    public QueryOp getType() {
        return type;
    }

    /**
     * Get the query.
     *
     * @return the subquery of the constraint
     */
    public Query getQuery() {
        return subquery;
    }

    /**
     * Get the QueryEvaluable the query is compared with
     *
     * @return QueryEvaluable
     */
    public QueryEvaluable getQueryEvaluable() {
        return qe;
    }

    /**
     * Get the QueryClass the query is compare with
     *
     * @return QueryClass
     */
    public QueryClass getQueryClass() {
        return cls;
    }

    /**
     * Returns a boolean whether or not the constraint is effectively "NOT IN", rather than "IN".
     *
     * @return true if the query is NOT IN
     */
    public boolean isNotIn() {
        return (type == CONTAINS ? negated : !negated);
    }

    /**
     * Test whether two SubqueryConstraints are equal, overrides Object.equals()
     *
     * @param obj the object to compare with
     * @return true if objects are equal
     */
    public boolean equals(Object obj) {
        if (obj instanceof SubqueryConstraint) {
            SubqueryConstraint sc = (SubqueryConstraint) obj;
            return subquery.equals(sc.subquery)
                && type == sc.type
                && negated == sc.negated
                && Util.equals(sc.qe, qe)
                && Util.equals(sc.cls, cls);
        }
        return false;
    }

    /**
     * Get the hashCode for this object overrides Object.hashCode()
     *
     * @return the hashCode
     */
    public int hashCode() {
        return subquery.hashCode()
            + 3 * type.hashCode()
            + 5 * (negated ? 1 : 0)
            + 7 * Util.hashCode(qe)
            + 11 * Util.hashCode(cls);
    }

    //-------------------------------------------------------------------------
    
    /**
     * require that argument is contained within select of subquery
     */
    public static final QueryOp CONTAINS = QueryOp.CONTAINS;

    /**
     * require that argument is not contained in select of subquery
     */
    public static final QueryOp DOES_NOT_CONTAIN = QueryOp.DOES_NOT_CONTAIN;

    protected static final QueryOp[] VALID_OPS = new QueryOp[] {CONTAINS, DOES_NOT_CONTAIN};

    /**
     * Return a list of the valid operations for constructing a constraint of this type
     * @return a List of operation codes
     */
    public static List validOps() {
        return Arrays.asList(VALID_OPS);
    }
}
