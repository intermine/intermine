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

import java.util.Date;

import org.flymine.util.Util;

/**
 * Represents a constraint between two QueryEvaluable types.  These are query elements
 * that can be resolved to a value - fields, expressions, aggregate functions and
 * constants.  Constraint type can be standard numeric comparison, IS_NULL, and also MATCHES
 * for simple string pattern matching.
 *
 * @author Richard Smith
 * @author Mark Woodbridge
 */
public class SimpleConstraint implements Constraint
{
    protected QueryEvaluable qe1;
    protected QueryEvaluable qe2;
    protected boolean negated;
    protected int type;

    /**
     * Construct a Constraint.  Check that java types of QueryEvaluables are compatible with the
     * constraint type selected.
     *
     * @param qe1 first QueryEvaluable for comparison
     * @param type define type of comparison
     * @param qe2 second QueryEvaluable for comparison
     * @throws IllegalArgumentException if type does not correspond to a defined operation
     */
    public SimpleConstraint(QueryEvaluable qe1, int type, QueryEvaluable qe2) {
        this(qe1, type, qe2, false);
    }

    /**
     * Construct a Constraint.  Check that java types of QueryEvaluables are compatible with the
     * constraint type selected.
     *
     * @param qe1 first QueryEvaluable for comparison
     * @param type define type of comparison
     * @param qe2 second QueryEvaluable for comparison
     * @param negated reverse constraint logic if true
     * @throws IllegalArgumentException if type does not correspond to a defined operation
     */
    public SimpleConstraint(QueryEvaluable qe1, int type, QueryEvaluable qe2, boolean negated) {
        if (qe1 == null) {
            throw new NullPointerException("qe1 cannot be null");
        }

        if (qe2 == null) {
            throw new NullPointerException("qe2 cannot be null");
        }

        if (type < 0 || type > 9) {
            throw new IllegalArgumentException("Invalid value for type: " + type);
        }

        if (!validComparison(qe1.getType(), type, qe2.getType())) {
            throw new IllegalArgumentException("Invalid comparison: " + qe1.getType().getName()
                                               + " " + getOpString(type) + " "
                                               + qe2.getType().getName());
        }

        this.qe1 = qe1;
        this.qe2 = qe2;
        this.negated = negated;
        this.type = type;
    }

    /**
     * Construct a Constraint.  Check that correct type of constraint is selected for
     * single QueryEvaluable constructor
     *
     * @param qe1 first QueryEvaluable for comparison
     * @param type define type of comparison
     * @throws IllegalArgumentException if type does not correspond to a defined operation
     */
    public SimpleConstraint(QueryEvaluable qe1, int type) {
        this(qe1, type, false);
    }

    /**
     * Construct a Constraint.  Check that correct type of constraint is selected for
     * single QueryEvaluable constructor
     *
     * @param qe1 first QueryEvaluable for comparison
     * @param type define type of comparison
     * @param negated reverse constraint logic if true
     * @throws IllegalArgumentException if type does not correspond to a defined operation
     */
    public SimpleConstraint(QueryEvaluable qe1, int type, boolean negated) {
        if (qe1 == null) {
            throw (new NullPointerException("qe1 cannot be null"));
        }

        if (type < 0 || type > 9) {
            throw new IllegalArgumentException("Invalid value for type: " + type);
        }

        if (!validComparison(qe1.getType(), type, null)) {
            throw new IllegalArgumentException("Invalid comparison: " + qe1.getType().getName()
                                               + " " + getOpString(type));
        }

        this.qe1 = qe1;
        this.qe2 = null;
        this.negated = negated;
        this.type = type;
    }

    /**
     * Set whether constraint is negated.  Negated reverses the logic of the constraint
     * i.e equals becomes not equals.
     *
     * @param negated true if constraint logic to be reversed
     */
    public void setNegated(boolean negated) {
        this.negated = negated;
    }

    /**
     * Test if constraint logic has been reversed
     *
     * @return true if constraint is negated
     */
    public boolean isNegated() {
        return negated;
    }

    /**
     * Get type of constraint
     *
     * @return integer value of operation type
     */
    public int getType() {
        return type;
    }

    /**
     * Get type of constraint, taking negated into account.
     *
     * @return integer value of operation type
     */
    public int getRealType() {
        return negate(type);
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
     * Returns the String representation of the operation.
     *
     * @return String representation
     */
    public String getOpString() {
        return getOpString(type);
    }

    /**
     * Test whether two SimpleConstraints are equal, overrides Object.equals()
     *
     * @param obj the object to compare with
     * @return true if objects are equal
     */
    public boolean equals(Object obj) {
        if (obj instanceof SimpleConstraint) {
            SimpleConstraint sc = (SimpleConstraint) obj;
            return qe1.equals(sc.qe1)
                    && type == sc.type
                    && negated == sc.negated
                    && Util.equals(qe2, sc.qe2);
        }
        return false;
    }

    /**
     * Get the hashCode for this object overrides Object.hashCode()
     *
     * @return the hashCode
     */
    public int hashCode() {
        return qe1.hashCode()
            + 3 * type 
            + 29 * (negated ? 1 : 0)
            + 31 * Util.hashCode(qe2);
    }

    //-------------------------------------------------------------------------
    
    /**
     * require that the two arguments are either equal numerically or are identical strings
     */
    public static final int EQUALS = 0;

    /**
     * require that the two arguments are not equal numerically or not identical strings
     */
    public static final int NOT_EQUALS = 1;

    /**
     * require that the first argument is less than the second (numeric only)
     */
    public static final int LESS_THAN = 2;

    /**
     * require that the first argument is less than or equal to the second (numeric only)
     */
    public static final int LESS_THAN_EQUALS = 3;

    /**
     * require that the first argument is greater than the second (numeric only)
     */
    public static final int GREATER_THAN = 4;

    /**
     * require that the first argument is greater than or equal to the second (numeric only)
     */
    public static final int GREATER_THAN_EQUALS = 5;

    /**
     * require that the first argument is a substring of the second (string only)
     */
    public static final int MATCHES = 6;

    /**
     * require that the first argument is not a substring of the second (string only)
     */
    public static final int DOES_NOT_MATCH = 7;

    /**
     * require that the specified argument is null
     */
    public static final int IS_NULL = 8;

    /**
     * require that the specified argument is not null
     */
    public static final int IS_NOT_NULL = 9;

    protected static final String[] OPERATIONS = {
        "=",
        "!=",
        "<",
        "<=",
        ">",
        ">=",
        "LIKE",
        "NOT LIKE",
        "IS NULL",
        "IS NOT NULL"};

    protected static final int[] NUMBER_OPS = {
        EQUALS,
        NOT_EQUALS,
        LESS_THAN,
        LESS_THAN_EQUALS,
        GREATER_THAN,
        GREATER_THAN_EQUALS};

    protected static final int[] DATE_OPS = NUMBER_OPS;

    protected static final int[] STRING_OPS = {
        EQUALS,
        NOT_EQUALS,
        MATCHES,
        DOES_NOT_MATCH};

    protected static final int[] BOOLEAN_OPS = {
        EQUALS,
        NOT_EQUALS};

    /**
     * Get type of constraint, taking negated into account.
     *
     * @param operator the operator to negate
     * @return integer value of operation type
     */
    protected static int negate(int operator) {
        switch(operator) {
        case EQUALS:
            return NOT_EQUALS;
        case NOT_EQUALS:
            return EQUALS;
        case LESS_THAN:
            return GREATER_THAN_EQUALS;
        case GREATER_THAN_EQUALS:
            return LESS_THAN;
        case GREATER_THAN:
            return LESS_THAN_EQUALS;
        case LESS_THAN_EQUALS:
            return GREATER_THAN;
        case MATCHES:
            return DOES_NOT_MATCH;
        case DOES_NOT_MATCH:
            return MATCHES;
        case IS_NULL:
            return IS_NOT_NULL;
        case IS_NOT_NULL:
            return IS_NULL;
        }
        return operator;
    }

    /**
     * Returns the String representation of the operation.
     *
     * @param type the operator code
     * @return String representation
     */
    public static String getOpString(int type) {
        return OPERATIONS[type];
    }

    /**
     * Check whether a comparison is valid i.e. the arguments are comparable types and the
     * the operator is permitted for those types
     * @param arg1 the first argument
     * @param operator how to compare the arguments
     * @param arg2 the second argument
     * @return whether the comparison is valid
     */
    public static boolean validComparison(Class arg1, int operator, Class arg2) {
        if (arg2 == null) {
            return operator == IS_NULL || operator == IS_NOT_NULL;
        }
        if (comparable(arg1, arg2)) {
            return contains(validOperators(arg1), operator);
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
    public static boolean comparable(Class arg1, Class arg2) {
        if (Number.class.isAssignableFrom(arg1) && Number.class.isAssignableFrom(arg2)
            || arg1.equals(String.class) && arg2.equals(String.class)
            || arg1.equals(Boolean.class) && arg2.equals(Boolean.class)
            || arg1.equals(Date.class) && arg2.equals(Date.class)) {
            return true;
        }
        return false;
    }

    /**
     * Return the list of valid (binary) operator codes given arguments of a specified type
     * @param arg the argument type
     * @return an array of character codes
     */
    public static int[] validOperators(Class arg) {
        if (Number.class.isAssignableFrom(arg)) {
            return NUMBER_OPS;
        } else if (String.class.equals(arg)) {
            return STRING_OPS;
        } else if (Boolean.class.equals(arg)) {
            return BOOLEAN_OPS;
        } else if (Date.class.equals(arg)) {
            return DATE_OPS;
        }
        return new int[0];
    }

    /**
     * Utility method to check whether an (int) array contains an int
     * @param array the array to inspect
     * @param element the value to look for
     * @return whether the array contains the element
     */
    protected static boolean contains(int[] array, int element) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == element) {
                return true;
            }
        }
        return false;
    }
}
