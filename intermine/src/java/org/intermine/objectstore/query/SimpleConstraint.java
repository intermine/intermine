package org.flymine.objectstore.query;

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

    /**
     * require that the two arguments are either equal numerically or are identical strings
     */
    public static final int EQUALS = 1;

    /**
     * require that the two arguments are not equal numerically or not identical strings
     */
    public static final int NOT_EQUALS = 2;

    /**
     * require that the first argument is less than the second (numeric only)
     */
    public static final int LESS_THAN = 3;

    /**
     * require that the first argument is less than or equal to the second (numeric only)
     */
    public static final int LESS_THAN_EQUALS = 4;

    /**
     * require that the first argument is greater than the second (numeric only)
     */
    public static final int GREATER_THAN = 5;

    /**
     * require that the first argument is greater than or equal to the second (numeric only)
     */
    public static final int GREATER_THAN_EQUALS = 6;

    /**
     * require that the first argument is a substring of the second (string only)
     */
    public static final int MATCHES = 7;

    /**
     * require that the first argument is not a substring of the second (string only)
     */
    public static final int DOES_NOT_MATCH = 8;

    /**
     * require that the specified argument is null
     */
    public static final int IS_NULL = 9;

    /**
     * require that the specified argument is not null
     */
    public static final int IS_NOT_NULL = 10;

    private static final String OPERATIONS[] = {"",
        " = ",
        " != ",
        " < ",
        " <= ",
        " > ",
        " >= ",
        " LIKE ",
        " NOT LIKE ",
        " IS NULL",
        " IS NOT NULL"};

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
            throw (new NullPointerException("qe1 cannot be null"));
        }
        if (qe2 == null) {
            throw (new NullPointerException("qe2 cannot be null"));
        }

        this.qe1 = qe1;
        this.qe2 = qe2;
        this.negated = negated;

        if ((type < 1) || (type > 10)) {
            throw (new IllegalArgumentException("Invalid value for type: " + type));
        }
        if ((type == IS_NULL) || (type == IS_NOT_NULL)) {
            throw (new IllegalArgumentException("Invalid number of arguments for operation: "
                                                + type));
        }
        Class qe1Type = qe1.getType();
        Class qe2Type = qe2.getType();

        if (Number.class.isAssignableFrom(qe1Type) && Number.class.isAssignableFrom(qe2Type)) {
            if (!(type == EQUALS || type == NOT_EQUALS
                  || type == LESS_THAN || type == LESS_THAN_EQUALS
                  || type == GREATER_THAN || type == GREATER_THAN_EQUALS)) {
                throw (new IllegalArgumentException("Invalid type for numeric arguments: " + type));
            }
        } else if (String.class.isAssignableFrom(qe1Type)
                   && String.class.isAssignableFrom(qe2Type)) {
            if (!(type == MATCHES || type == DOES_NOT_MATCH
                  || type == EQUALS || type == NOT_EQUALS)) {
                throw (new IllegalArgumentException("Invalid type for string arguments: " + type));
            }
        } else {
            throw (new IllegalArgumentException("Invalid pair of arguments: " + qe1Type + qe2Type));
        }

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

        this.qe1 = qe1;
        this.qe2 = null;
        this.negated = negated;

        if ((type < 1) || (type > 10)) {
            throw (new IllegalArgumentException("Invalid value for type: " + type));
        }
        if ((type != IS_NULL) && (type != IS_NOT_NULL)) {
            throw (new IllegalArgumentException("Invalid number of arguments for operation: "
                                                + type));
        }
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
        if (negated) {
            switch(type) {
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
        }
        return type;
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
        return OPERATIONS[getRealType()];
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
            return (qe1.equals(sc.qe1)
                    && (type == sc.type)
                    && (negated == sc.negated)
                    && ((qe2 != null) ? (qe2.equals(sc.qe2)) : (sc.qe2 == null)));
        }
        return false;
    }
    /**
     * Get the hashCode for this object overrides Object.hashCode()
     *
     * @return the hashCode
     */
    public int hashCode() {
        return qe1.hashCode() + (3 * type) + ((negated) ? 29 : 0)
            + ((qe2 != null) ? qe2.hashCode() : 31);
    }

}
