package org.flymine.objectstore.query;

/**
 * Represents an arithmetic or substring expression, analogous to those in SQL
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 * @author Matthew Wakeling
 */
public class QueryExpression implements QueryEvaluable
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
    private QueryEvaluable arg3;
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
     * Constructs a substring QueryExpression from a QueryEvaluable and start and length
     * QueryEvaluables
     *
     * @param arg A QueryEvaluable representing a String
     * @param pos start index
     * @param len length in characters
     * @throws IllegalArgumentException if there is a mismatch between the argument type 
     * and the specified operation
     */    
    public QueryExpression(QueryEvaluable arg, QueryEvaluable pos, QueryEvaluable len) 
        throws IllegalArgumentException {
        if (!(arg.getType().equals(String.class))) {
            throw new IllegalArgumentException("Invalid argument type for specified operation");
        }
        if (!(Number.class.isAssignableFrom(pos.getType()))) {
            throw new IllegalArgumentException("Invalid argument type pos for substring");
        }
        if (!(Number.class.isAssignableFrom(len.getType()))) {
            throw new IllegalArgumentException("Invalid argument type len for substring");
        }
        if ((pos instanceof QueryValue) && (((Number) ((QueryValue) pos).getValue()).longValue()
                    < 0)) {
            throw (new IllegalArgumentException("Invalid pos argument less than zero for "
                        + "substring"));
        }
        if ((len instanceof QueryValue) && (((Number) ((QueryValue) len).getValue()).longValue()
                    < 0)) {
            throw (new IllegalArgumentException("Invalid len argument less than zero for "
                        + "substring"));
        }
        arg1 = arg;
        op = SUBSTRING;
        arg2 = pos;
        arg3 = len;
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
    public QueryEvaluable getArg3() {
        return arg3;
    }
}
