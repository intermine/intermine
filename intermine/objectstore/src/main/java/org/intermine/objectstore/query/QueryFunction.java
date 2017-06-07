package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * A QueryFunction represents an aggregate
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 * @author Matthew Wakeling
 */
public class QueryFunction implements QueryEvaluable
{
    /**
     * Sum of a number of rows
     */
    public static final int SUM = 0;
    /**
     * Average value of a number of rows
     */
    public static final int AVERAGE = 1;
    /**
     * Mininum value of a number of rows
     */
    public static final int MIN = 2;
    /**
     * Maximum value of a number of rows
     */
    public static final int MAX = 3;
    /**
     * Count rows
     */
    public static final int COUNT = 4;
    /**
     * Sample standard deviation of a number of rows
     */
    public static final int STDDEV = 5;

    /**
     * Smallest integer value greater than input.
     */
    public static final int CEIL = 6;

    /**
     * Greatest integer value less than input.
     */
    public static final int FLOOR = 7;

    /**
     * Round to a given number of decimal places.
     */
    public static final int ROUND = 9;

    /**
     * Get the bucket this value would be assigned in an equal-depth histogram.
     */
    public static final int WIDTH_BUCKET = 10;

    protected QueryEvaluable obj;
    protected int op;

    private QueryEvaluable obj2;

    /**
     * @param qe the QueryEvaluable to aggregate over
     * @param op the operation code
     * @throws IllegalArgumentException if there is a mismatch between the argument type
     * and the specified operation
     */
    public QueryFunction(QueryEvaluable qe, int op) {
        if ((qe instanceof QueryField) || (qe instanceof QueryExpression)
                || (qe instanceof QueryCast) || (qe instanceof QueryForeignKey)) {
            constructNonCount(qe, op);
        } else {
            throw new IllegalArgumentException("Value unsuitable for QueryFunction: " + qe);
        }
    }

    /**
     * Constructor for functions that take two parameters.
     * @param qe The first parameter.
     * @param op The operation code.
     * @param qe2 The second parameter.
     */
    public QueryFunction(QueryEvaluable qe, int op, QueryEvaluable qe2) {
        this(qe, op);
        if ((qe instanceof QueryField) || (qe instanceof QueryExpression)
                || (qe instanceof QueryCast) || (qe instanceof QueryForeignKey)) {
            if (!(Integer.class.isAssignableFrom(qe2.getType())
                    || qe2.getType().equals(UnknownTypeValue.class))) {
                throw new IllegalArgumentException("Invalid parameter argument type for "
                          + "specified operation");
            }
            obj2 = qe2;
        } else {
            throw new IllegalArgumentException("Parameter Value unsuitable for QueryFunction: "
                    + qe2);
        }
    }

    /**
     * Creates a COUNT aggregate function.
     */
    public QueryFunction() {
        op = COUNT;
        obj = null;
    }

    /**
       * {@inheritDoc}
       */
    @Override
    public Class<?> getType() {
        if (op == COUNT) {
            return Long.class;
        }
        return obj.getType();
    }

    /**
     * Returns the operation of the function.
     *
     * @return the operation
     */
    public int getOperation() {
        return op;
    }

    /**
     * Returns the QueryEvaluable of the function.
     *
     * @return the QueryEvaluable
     */
    public QueryEvaluable getParam() {
        return obj;
    }

    /**
     * Returns the second evaluable, where these is one.
     * @return The second evaluable.
     */
    public QueryEvaluable getParam2() {
        return obj2;
    }

    private void constructNonCount(QueryEvaluable qe, int newOp) {
        if (!(op == SUM || op == AVERAGE || op == MIN || op == MAX || op == STDDEV
                || op == CEIL || op == FLOOR || op == ROUND)) {
            throw new IllegalArgumentException("Invalid operation for specified argument");
        }
        if (!(Number.class.isAssignableFrom(qe.getType())
              || qe.getType().equals(UnknownTypeValue.class))) {
            throw new IllegalArgumentException("Invalid argument type for specified operation");
        }
        obj = qe;
        this.op = newOp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void youAreType(Class<?> cls) {
        if (obj.getType().equals(UnknownTypeValue.class)) {
            obj.youAreType(cls);
        } else {
            throw new ClassCastException("youAreType called on function that already has type");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getApproximateType() {
        if (obj.getType().equals(UnknownTypeValue.class)) {
            return obj.getApproximateType();
        } else {
            throw new ClassCastException("getApproximateType called when type is known");
        }
    }
}
