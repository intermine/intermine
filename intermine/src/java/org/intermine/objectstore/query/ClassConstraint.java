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
 * Constrain whether a QueryClass is equal/not equal to another
 * QueryClass or an example of an object belonging to a
 * QueryClass. Note: QueryClass = QueryClass makes no sense, but is
 * allowed.
 *
 * @author Richard Smith
 * @author Mark Woodbridge
 * @author Andrew Varley
 */
public class ClassConstraint extends Constraint
{
    protected QueryClass qc1, qc2;
    protected ConstraintOp type;
    protected Object obj;

    /**
     * Construct ClassConstraint
     *
     * @param qc1 first QueryClass for comparison
     * @param type define EQUALS or NOT_EQUALS
     * @param qc2 second QueryClass for comparison
     */
    public ClassConstraint(QueryClass qc1, ConstraintOp type, QueryClass qc2) {
        this(qc1, type, qc2, false);
    }

    /**
     * Construct ClassConstraint
     *
     * @param qc1 first QueryClass for comparison
     * @param type define EQUALS or NOT_EQUALS
     * @param qc2 second QueryClass for comparison
     * @param negated reverse the constraint logic if true
     */
    public ClassConstraint(QueryClass qc1, ConstraintOp type, QueryClass qc2, boolean negated) {
        if (qc1 == null) {
            throw new NullPointerException("qc1 cannot be null");
        }

        if (type == null) {
            throw new NullPointerException("type cannot be null");
        }
        
        if (!validOps().contains(type)) {
            throw new NullPointerException("type cannot be " + type);
        }

        if (qc2 == null) {
            throw new NullPointerException("qc2 cannot be null");
        }
        
        if (!(qc1.getType().isAssignableFrom(qc2.getType())
              || qc2.getType().isAssignableFrom(qc1.getType()))) {
            throw new IllegalArgumentException("Invalid constraint: "
                                               + qc1.getType()
                                               + " " + type
                                               + " " + qc2.getType());
        }
        
        this.qc1 = qc1;
        this.type = type;
        this.qc2 = qc2;
        this.negated = negated;
    }

    /**
     * Construct ClassConstraint
     *
     * @param qc QueryClass for comparison
     * @param type define EQUALS or NOT_EQUALS
     * @param obj example object
     */
    public ClassConstraint(QueryClass qc, ConstraintOp type, Object obj) {
        this(qc, type, obj, false);
    }

    /**
     * Construct ClassConstraint
     *
     * @param qc1 QueryClass for comparison
     * @param type define EQUALS or NOT_EQUALS
     * @param obj example object
     * @param negated reverse the constraint logic if true
     */
    public ClassConstraint(QueryClass qc1, ConstraintOp type, Object obj, boolean negated) {
        if (qc1 == null) {
            throw new NullPointerException("obj cannot be null");
        }

        if (type == null) {
            throw new NullPointerException("type cannot be null");
        }

        if (!validOps().contains(type)) {
            throw new NullPointerException("type cannot be " + type);
        }

        if (obj == null) {
            throw new NullPointerException("obj cannot be null");
        }

        if (!(qc1.getType().isAssignableFrom(obj.getClass())
              || obj.getClass().isAssignableFrom(qc1.getType()))) {
              throw new IllegalArgumentException("Invalid constraint: "
                                               + qc1.getType()
                                               + " " + type
                                               + " " + obj.getClass());
        }
        
        this.qc1 = qc1;
        this.type = type;
        this.obj = obj;
        this.negated = negated;
    }

    /**
     * Return the operation type
     *
     * @return the operation type
     */
    public ConstraintOp getType() {
        return type;
    }

    /**
     * Returns the QueryClass argument 1
     *
     * @return QueryClass arg1
     */
    public QueryClass getArg1() {
        return qc1;
    }

    /**
     * Returns the QueryClass argument 2
     *
     * @return QueryClass arg2
     */
    public QueryClass getArg2QueryClass() {
        return qc2;
    }

    /**
     * Returns the Object argument 2
     *
     * @return Object arg2
     */
    public Object getArg2Object() {
        return obj;
    }

    /**
     * Tests whether two ClassConstraints are equal.
     *
     * @param o the object to compare with
     * @return true if objects are equal
     */
    public boolean equals(Object o) {
        if (o instanceof ClassConstraint) {
            ClassConstraint cc = (ClassConstraint) o;
            return  qc1.equals(cc.qc1)
                && type == cc.type
                && negated == cc.negated
                && Util.equals(cc.qc2, qc2)
                && Util.equals(cc.obj, obj);
        }
        return false;
    }

    /**
     * Get the hashCode for this object
     *
     * @return the hashCode
     */
    public int hashCode() {
        return qc1.hashCode()
            + 3 * type.hashCode()
            + 5 * Util.hashCode(qc2)
            + 7 * Util.hashCode(obj)
            + 11 * (negated ? 1 : 0);
    }

    /**
     * Returns a boolean whether or not the constraint is effectively "NOT EQUALS", rather than
     * "EQUALS".
     *
     * @return true if the the query is NOT EQUALS
     */
    public boolean isNotEqual() {
        return (type == EQUALS ? negated : !negated);
    }

    //-------------------------------------------------------------------------
    
    /**
     * Classes are equal to one another
     */
    public static final ConstraintOp EQUALS = ConstraintOp.EQUALS;
    
    /**
     * Classes are not equal to one another
     */
    public static final ConstraintOp NOT_EQUALS = ConstraintOp.NOT_EQUALS;

    protected static final ConstraintOp[] VALID_OPS = new ConstraintOp[] {EQUALS, NOT_EQUALS};

    /**
     * Return a list of the valid operations for constructing a constraint of this type
     * @return a List of operation codes
     */
    public static List validOps() {
        return Arrays.asList(VALID_OPS);
    }
}
