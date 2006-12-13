package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2005 FlyMine
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

    private QueryEvaluable obj;
    private int op;

    /**
     * @param qf the QueryField to aggregate over
     * @param op the operation code
     * @throws IllegalArgumentException if there is a mismatch between the argument type
     * and the specified operation
     */
    public QueryFunction(QueryField qf, int op) throws IllegalArgumentException {
        constructNonCount(qf, op);
    }

    /**
     * @param qe the QueryExpression to aggregate over
     * @param op the operation code
     * @throws IllegalArgumentException if there is a mismatch between the argument type
     * and the specified operation
     */
    public QueryFunction(QueryExpression qe, int op) throws IllegalArgumentException {
        constructNonCount(qe, op);
    }

    /**
     * @param qc the QueryCast to aggregate over
     * @param op the operation code
     * @throws IllegalArgumentException if there is a mismatch between the argument type
     * and the specified operation
     */
    public QueryFunction(QueryCast qc, int op) throws IllegalArgumentException {
        constructNonCount(qc, op);
    }

    /**
     * Creates a COUNT aggregate function.
     */
    public QueryFunction() {
        op = COUNT;
        obj = null;
    }

    /**
       * @see QueryEvaluable
       */
    public Class getType() {
        if (op == COUNT) {
            return Integer.class;
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

    private void constructNonCount(QueryEvaluable qe, int op) throws IllegalArgumentException {
        if (!(op == SUM || op == AVERAGE || op == MIN || op == MAX || op == STDDEV)) {
            throw new IllegalArgumentException("Invalid operation for specified argument");
        }
        if (!(Number.class.isAssignableFrom(qe.getType()) 
              || qe.getType().equals(UnknownTypeValue.class))) {
            throw new IllegalArgumentException("Invalid argument type for specified operation");
        }
        obj = qe;
        this.op = op;
    }

    /**
     * @see QueryEvaluable#youAreType
     */
    public void youAreType(Class cls) {
        if (obj.getType().equals(UnknownTypeValue.class)) {
            obj.youAreType(cls);
        } else {
            throw new ClassCastException("youAreType called on function that already has type");
        }
    }

    /**
     * @see QueryEvaluable#getApproximateType
     */
    public int getApproximateType() {
        if (obj.getType().equals(UnknownTypeValue.class)) {
            return obj.getApproximateType();
        } else {
            throw new ClassCastException("getApproximateType called when type is known");
        }
    }
}
