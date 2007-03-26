package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Arrays;

import org.intermine.util.Util;

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
    protected QueryEvaluable qe;
    protected QueryClass cls;    

    /**
     * Construct a SubqueryConstraint with a QueryEvaluable
     *
     * @param qe item to match against subquery select
     * @param op required op of constraint
     * @param query the subquery in question
     */
    public SubqueryConstraint(QueryEvaluable qe, ConstraintOp op, Query query) {
        if (query == null) {
            throw new NullPointerException("query cannot be null");
        }
  
        if (op == null) {
            throw new NullPointerException("op cannot be null");
        }

        if (!VALID_OPS.contains(op)) {
            throw new IllegalArgumentException("op cannot be " + op);
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
        QuerySelectable selectNode = (QuerySelectable) select.get(0);
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
        this.op = op;
        this.qe = qe;
    }

    /**
     * Construct a SubqueryConstraint with a QueryClass
     *
     * @param cls item to match against subquery select
     * @param op required op of constraint
     * @param query the subquery in question
     */
    public SubqueryConstraint(QueryClass cls, ConstraintOp op, Query query) {
        if (query == null) {
            throw new NullPointerException("query cannot be null");
        }

        if (op == null) {
            throw new NullPointerException("op cannot be null");
        }

        if (!VALID_OPS.contains(op)) {
            throw new NullPointerException("op cannot be " + op);
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

        QuerySelectable selectNode = (QuerySelectable) select.get(0);
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
        this.op = op;
        this.cls = cls;
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
     * Test whether two SubqueryConstraints are equal, overrides Object.equals()
     *
     * @param obj the object to compare with
     * @return true if objects are equal
     */
    public boolean equals(Object obj) {
        if (obj instanceof SubqueryConstraint) {
            SubqueryConstraint sc = (SubqueryConstraint) obj;
            return subquery.equals(sc.subquery)
                && op == sc.op
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
            + 3 * op.hashCode()
            + 7 * Util.hashCode(qe)
            + 11 * Util.hashCode(cls);
    }

    //-------------------------------------------------------------------------
    
    protected static final List VALID_OPS = Arrays.asList(new ConstraintOp[] {ConstraintOp.IN,
        ConstraintOp.NOT_IN});
}
