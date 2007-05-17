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

import java.math.BigDecimal;
import java.util.Date;
import java.util.Arrays;
import java.util.List;
import org.intermine.util.Util;

/**
 * Represents a constraint between two QueryEvaluable types.  These are query elements
 * that can be resolved to a value - fields, expressions, aggregate functions and
 * constants.  Constraint ops can be standard numeric comparison, IS_NULL, and also MATCHES
 * for simple string pattern matching.
 *
 * @author Richard Smith
 * @author Mark Woodbridge
 * @author Matthew Wakeling
 */
public class SimpleConstraint extends Constraint
{
    protected QueryEvaluable qe1, qe2;

    /**
     * Construct a Constraint.  Check that java types of QueryEvaluables are compatible with the
     * constraint type selected.
     *
     * @param qe1 first QueryEvaluable for comparison
     * @param op define comparison op
     * @param qe2 second QueryEvaluable for comparison
     * @throws IllegalArgumentException if type does not correspond to a defined operation
     */
    public SimpleConstraint(QueryEvaluable qe1, ConstraintOp op, QueryEvaluable qe2) {
        if (qe1 == null) {
            throw new NullPointerException("qe1 cannot be null");
        }

        if (op == null) {
            throw new NullPointerException("op cannot be null");
        }

        if (qe2 == null) {
            throw new NullPointerException("qe2 cannot be null");
        }

        if (qe1.getType().equals(UnknownTypeValue.class)
                && (!(qe2.getType().equals(UnknownTypeValue.class)))) {
            qe1.youAreType(qe2.getType());
        }
        if (qe2.getType().equals(UnknownTypeValue.class)
                && (!(qe1.getType().equals(UnknownTypeValue.class)))) {
            qe2.youAreType(qe1.getType());
        }
        if (!validComparison(qe1.getType(), op, qe2.getType())) {
            throw new IllegalArgumentException("Invalid constraint: " + qe1 + " (a "
                    + qe1.getType().getName() + ") " + op + " " + qe2 + " (a "
                    + qe2.getType().getName() + ")");
        }

        this.qe1 = qe1;
        this.op = op;
        this.qe2 = qe2;
    }

    /**
     * Construct a Constraint.  Check that correct constraint op is selected for
     * single QueryEvaluable constructor
     *
     * @param qe1 first QueryEvaluable for comparison
     * @param op define op of comparison
     * @throws IllegalArgumentException if op does not correspond to a defined operation
     */
    public SimpleConstraint(QueryEvaluable qe1, ConstraintOp op) {
        if (qe1 == null) {
            throw new NullPointerException("qe1 cannot be null");
        }

        if (op == null) {
            throw new NullPointerException("op cannot be null");
        }

        if (!validComparison(qe1.getType(), op, null)) {
            throw new IllegalArgumentException("Invalid constraint: "
                                               + qe1.getType().getName()
                                               + " " + op);
        }

        this.qe1 = qe1;
        this.op = op;
    }

    /**
     * Returns the left argument of the constraint.
     *
     * @return the left-hand argument
     */
    public QueryEvaluable getArg1() {
        return qe1;
    }

    /**
     * Returns the right argument of the constraint.
     *
     * @return the right-hand argument
     */
    public QueryEvaluable getArg2() {
        return qe2;
    }

    /**
     * Test whether two SimpleConstraints are equal, overrides Object.equals()
     *
     * @param obj the object to compare with
     * @return true if objects are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SimpleConstraint) {
            SimpleConstraint sc = (SimpleConstraint) obj;
            return qe1.equals(sc.qe1)
                    && op == sc.op
                    && Util.equals(qe2, sc.qe2);
        }
        return false;
    }

    /**
     * Get the hashCode for this object overrides Object.hashCode()
     *
     * @return the hashCode
     */
    @Override
    public int hashCode() {
        return qe1.hashCode()
            + 3 * op.hashCode() 
            + 7 * Util.hashCode(qe2);
    }

    protected static final List<ConstraintOp> NUMBER_OPS = Arrays.asList(new ConstraintOp[] {
        ConstraintOp.EQUALS,
        ConstraintOp.NOT_EQUALS,
        ConstraintOp.LESS_THAN,
        ConstraintOp.LESS_THAN_EQUALS,
        ConstraintOp.GREATER_THAN,
        ConstraintOp.GREATER_THAN_EQUALS});

    protected static final List<ConstraintOp> DATE_OPS = NUMBER_OPS;

    protected static final List<ConstraintOp> STRING_OPS = Arrays.asList(new ConstraintOp[] {
        ConstraintOp.EQUALS,
        ConstraintOp.NOT_EQUALS,
        ConstraintOp.LESS_THAN,
        ConstraintOp.LESS_THAN_EQUALS,
        ConstraintOp.GREATER_THAN,
        ConstraintOp.GREATER_THAN_EQUALS,
        ConstraintOp.MATCHES,
        ConstraintOp.DOES_NOT_MATCH});

    protected static final List<ConstraintOp> BOOLEAN_OPS = Arrays.asList(new ConstraintOp[] {
        ConstraintOp.EQUALS,
        ConstraintOp.NOT_EQUALS});

    protected static final List<ConstraintOp> ALL_OPS = Arrays.asList(new ConstraintOp[] {
        ConstraintOp.EQUALS,
        ConstraintOp.NOT_EQUALS,
        ConstraintOp.LESS_THAN,
        ConstraintOp.LESS_THAN_EQUALS,
        ConstraintOp.GREATER_THAN,
        ConstraintOp.GREATER_THAN_EQUALS,
        ConstraintOp.MATCHES,
        ConstraintOp.DOES_NOT_MATCH});

    /**
     * Check whether a comparison is valid i.e. the arguments are comparable types and the
     * the operator is permitted for those types
     * @param arg1 the first argument
     * @param op how to compare the arguments
     * @param arg2 the second argument
     * @return whether the comparison is valid
     */
    public static boolean validComparison(Class<?> arg1, ConstraintOp op, Class<?> arg2) {
        if (arg2 == null) {
            return op == ConstraintOp.IS_NULL || op == ConstraintOp.IS_NOT_NULL;
        }
        if (comparable(arg1, arg2)) {
            return validOps(arg1).contains(op);
        }
        return false;
    }

    /**
     * Check whether the two arguments are of comparable types i.e. they are of similiar type
     * and we know how to handle that type
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @return whether the types are comparable
     */
    public static boolean comparable(Class<?> arg1, Class<?> arg2) {
        if (arg1.equals(arg2)
                && (arg1.equals(String.class) || arg1.equals(Boolean.class)
                    || arg1.equals(Date.class) || arg1.equals(Short.class)
                    || arg1.equals(Integer.class) || arg1.equals(Long.class)
                    || arg1.equals(Float.class) || arg1.equals(Double.class)
                    || arg1.equals(BigDecimal.class) || arg1.equals(UnknownTypeValue.class))) {
            return true;
        }
        return false;
    }

    /**
     * Return the list of valid (binary) operator codes given arguments of a specified type
     * @param arg the argument type
     * @return an array of character codes
     */
    public static List<ConstraintOp> validOps(Class<?> arg) {
        if (Number.class.isAssignableFrom(arg)) {
            return NUMBER_OPS;
        } else if (String.class.equals(arg)) {
            return STRING_OPS;
        } else if (Boolean.class.equals(arg)) {
            return BOOLEAN_OPS;
        } else if (Date.class.equals(arg)) {
            return DATE_OPS;
        } else if (UnknownTypeValue.class.equals(arg)) {
            return ALL_OPS;
        }
        return null;
    }
    
    /**
     * For an argument type which an enumerated value set, return the list of
     * operators for which it makes sense only to provide the enumerated values
     * and not allow the user to enter an arbitrary string.
     *
     * @param arg the argument type
     * @return  constraint operators that will only accept an enumerated value
     */
    public static List<ConstraintOp> fixedEnumOps(Class<?> arg) {
        if (Number.class.isAssignableFrom(arg)) {
            return NUMBER_OPS;
        } else if (String.class.equals(arg)) {
            return BOOLEAN_OPS;
        } else if (Boolean.class.equals(arg)) {
            return BOOLEAN_OPS;
        } else if (Date.class.equals(arg)) {
            return DATE_OPS;
        } else {
            return ALL_OPS;
        }
    }
}
