package org.flymine.objectstore.query;

/**
 * A QueryFunction represents an aggregate
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public class QueryFunction implements QueryNode, QueryEvaluable
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

    private Object obj;
    private int op;
    
    /**
     * @return the operation code
     */    
    public int getOp() {
        return op;
    }
    
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
     * @param qc the QueryClass to aggregate over
     * @throws IllegalArgumentException if there is a mismatch between the argument type 
     * and the specified operation
     */    
    public QueryFunction(QueryClass qc) throws IllegalArgumentException {
        op = COUNT;
        obj = qc;
    }

    /**
       * @see QueryEvaluable
       */
    public Class getType() {
        return Number.class;
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
