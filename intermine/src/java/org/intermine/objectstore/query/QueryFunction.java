package org.flymine.objectstore.query;

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
     * Sum of a number of QueryFields or QueryExpressions
     */
    public static final int SUM = 0;
    /**
     * Average of a number of QueryFields or QueryExpressions
     */
    public static final int AVERAGE = 1;
    /**
     * Mininum of a number of QueryFields or QueryExpressions
     */
    public static final int MIN = 2;
    /**
     * Maximum of a number of QueryFields or QueryExpressions
     */
    public static final int MAX = 3;
    /**
     * Count over a number of QueryClasses
     */
    public static final int COUNT = 4;

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
        return Number.class;
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
        if (!(op == SUM || op == AVERAGE || op == MIN || op == MAX)) {
            throw new IllegalArgumentException("Invalid operation for specified argument");
        }
        if (!(Number.class.isAssignableFrom(qe.getType()))) {
            throw new IllegalArgumentException("Invalid argument type for specified operation");
        }
        obj = qe;
        this.op = op;
    }
}
