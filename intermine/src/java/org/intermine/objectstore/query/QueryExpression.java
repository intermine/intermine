package org.flymine.objectstore.query;

/**
 * Represents an arithmetic or substring expression, analogous to those in SQL
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public class QueryExpression implements QueryNode, QueryEvaluable
{
    /**
     * Addition of two numeric fields
     */
    public static final int ADD = 0;
    /**
     * Subtraction of two numeric fields
     */
    public static final int SUBTRACT = 1;
    /**
     * Multiplication of two numeric fields
     */
    public static final int MULTIPLY = 2;
    /**
     * Division of two numeric fields
     */
    public static final int DIVIDE = 3;
    /**
     * Substring of specified length from index in string
     */
    public static final int SUBSTRING = 4;
    
    private QueryEvaluable arg1;
    private int op;
    private QueryEvaluable arg2;
    private QueryValue arg3;
    private Class type;
    
    /**
     * Constructs an arithmetic QueryExpression from two evaluable items
     *
     * @param arg1 the first argument
     * @param op the required operation
     * @param arg2 the second argument
     * @throws IllegalArgumentException if there is a mismatch between any of the argument
     * types and the specified operation
     */    
    public QueryExpression(QueryEvaluable arg1, int op, QueryEvaluable arg2) 
        throws IllegalArgumentException {
        if (!(Number.class.isAssignableFrom(arg1.getType()) 
              && Number.class.isAssignableFrom(arg2.getType()))) {
            throw new IllegalArgumentException("Invalid arguments for specified operation");
        }
        if (!(op == ADD || op == SUBTRACT || op == MULTIPLY || op == DIVIDE)) {
            throw new IllegalArgumentException("Invalid operation for specified arguments");
        }
        this.arg1 = arg1;
        this.op = op;
        this.arg2 = arg2;
        this.type = Number.class;
    }
    
    /**
     * Constructs a substring QueryExpression from a QueryField and start and length values
     *
     * @param arg A QueryField representing a String
     * @param pos start index
     * @param len length in characters
     * @throws IllegalArgumentException if there is a mismatch between the argument type 
     * and the specified operation
     */    
    public QueryExpression(QueryField arg, int pos, int len) 
        throws IllegalArgumentException {
        constructSubstring(arg, pos, len);
    }
    
    /**
     * Constructs a substring QueryExpression from a QueryExpression and start and length values
     *
     * @param arg A QueryExpression representing a String
     * @param pos start index
     * @param len length in characters
     * @throws IllegalArgumentException if there is a mismatch between the argument type 
     * and the specified operation
     */    
    public QueryExpression(QueryExpression arg, int pos, int len) 
        throws IllegalArgumentException {
        constructSubstring(arg, pos, len);
    }
    
    /**
     * @param arg A QueryEvaluable representing a String type
     * @param pos start index
     * @param len length in characters
     * @throws
     */ 
    private void constructSubstring(QueryEvaluable arg, int pos, int len)
        throws IllegalArgumentException {
        if (!(arg.getType().equals(String.class))) {
            throw new IllegalArgumentException("Invalid argument type for specified operation");
        }
        if (pos < 0 || len < 0) {
            throw new IllegalArgumentException("Start position or length is less than zero");
        }
        arg1 = arg;
        op = SUBSTRING;
        arg2 = new QueryValue(new Integer(pos));
        arg3 = new QueryValue(new Integer(len));
        type = String.class;
    }

    /**
       * @see QueryEvaluable
       */
    public Class getType() {
        return type;
    }

    /**
     * Returns the operation.
     *
     * @return the operation of the expression
     */
    public int getOperation() {
        return op;
    }

    /**
     * Returns the left argument of the expression.
     *
     * @return the left argument
     */
    public QueryEvaluable getArg1() {
        return arg1;
    }

    /**
     * Returns the right argument, or the position argument of the substring.
     *
     * @return argument 2
     */
    public QueryEvaluable getArg2() {
        return arg2;
    }

    /**
     * Returns the length argument of a substring expression.
     *
     * @return argument 3
     */
    public QueryValue getArg3() {
        return arg3;
    }
}
