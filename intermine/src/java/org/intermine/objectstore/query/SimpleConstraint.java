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
        this.negated = false;

        if ((type < 1) || (type > 8)) {
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
}
