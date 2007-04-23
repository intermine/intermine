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
    /**
     * Position of specified string in other specified string
     */
    public static final int INDEX_OF = 5;
    /**
     * Lower case version of the given string
     */
    public static final int LOWER = 6;
    /**
     * Upper case version of the given string
     */
    public static final int UPPER = 7;
    
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
        if (op == ADD || op == SUBTRACT || op == MULTIPLY || op == DIVIDE) {
            if (Number.class.isAssignableFrom(arg1.getType()) 
                    && Number.class.isAssignableFrom(arg2.getType())
                    && arg1.getType().equals(arg2.getType())) {
                this.type = arg1.getType();
            } else if (arg1.getType().equals(UnknownTypeValue.class)
                    && (!(arg2.getType().equals(UnknownTypeValue.class)))) {
                arg1.youAreType(arg2.getType());
                this.type = arg1.getType();
            } else if (arg2.getType().equals(UnknownTypeValue.class)
                    && (!(arg1.getType().equals(UnknownTypeValue.class)))) {
                arg2.youAreType(arg1.getType());
                this.type = arg2.getType();
            } else if ((arg1.getType().equals(UnknownTypeValue.class))
                    && (arg2.getType().equals(UnknownTypeValue.class))) {
                if (arg1.getApproximateType() != arg2.getApproximateType()) {
                    throw new ClassCastException("Incompatible expression with unknown type"
                            + " values");
                }
                this.type = UnknownTypeValue.class;
            } else {
                throw new ClassCastException("Invalid arguments (" + arg1.getType() + ", "
                        + arg2.getType() + ") for specified operation");
            }
        } else if (op == INDEX_OF) {
            if (arg1.getType().equals(UnknownTypeValue.class)) {
                arg1.youAreType(String.class);
            }
            if (arg2.getType().equals(UnknownTypeValue.class)) {
                arg2.youAreType(String.class);
            }
            if (String.class.isAssignableFrom(arg1.getType())
                    && String.class.isAssignableFrom(arg2.getType())) {
                this.type = Integer.class;
            } else {
                throw new ClassCastException("Invalid arguments (" + arg1.getType() + ", "
                        + arg2.getType() + ") for indexof operation");
            }
        } else if (op == SUBSTRING) {
            if (arg1.getType().equals(UnknownTypeValue.class)) {
                arg1.youAreType(String.class);
            } else if (!arg1.getType().equals(String.class)) {
                throw new ClassCastException("Invalid arguments (" + arg1.getType() + ", "
                        + arg2.getType() + ") for substring operation");
            }
            if (arg2.getType().equals(UnknownTypeValue.class)) {
                arg2.youAreType(Integer.class);
            } else if (!Number.class.isAssignableFrom(arg2.getType())) {
                throw new ClassCastException("Invalid arguments (" + arg1.getType() + ", "
                        + arg2.getType() + ") for substring operation");
            }
            if ((arg2 instanceof QueryValue) && (((Integer) ((QueryValue) arg2).getValue())
                        .intValue() <= 0)) {
                throw (new IllegalArgumentException("Invalid pos argument less than or equal to"
                            + " zero for substring"));
            }
            this.type = String.class;
        } else {
            throw new IllegalArgumentException("Invalid operation for specified arguments");
        }
        this.arg1 = arg1;
        this.op = op;
        this.arg2 = arg2;
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
        if (arg.getType().equals(UnknownTypeValue.class)) {
            arg.youAreType(String.class);
        } else if (!arg.getType().equals(String.class)) {
            throw new ClassCastException("Invalid arguments (" + arg.getType() + ", "
                    + pos.getType() + ", " + len.getType() + ") for substring operation");
        }
        if (pos.getType().equals(UnknownTypeValue.class)) {
            pos.youAreType(Integer.class);
        } else if (!Number.class.isAssignableFrom(pos.getType())) {
            throw new ClassCastException("Invalid arguments (" + arg.getType() + ", "
                    + pos.getType() + ", " + len.getType() + ") for substring operation");
        }
        if (len.getType().equals(UnknownTypeValue.class)) {
            len.youAreType(Integer.class);
        } else if (!Number.class.isAssignableFrom(len.getType())) {
            throw new ClassCastException("Invalid arguments (" + arg.getType() + ", "
                    + pos.getType() + ", " + len.getType() + ") for substring operation");
        }
        if ((pos instanceof QueryValue) && (((Integer) ((QueryValue) pos).getValue()).intValue()
                    <= 0)) {
            throw (new IllegalArgumentException("Invalid pos argument less than or equal to zero"
                        + " for substring"));
        }
        if ((len instanceof QueryValue) && (((Integer) ((QueryValue) len).getValue()).intValue()
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
     * Constructs a String QueryExpression to perform upper and lowercase conversions.
     *
     * @param op the required operation
     * @param arg the String argument
     * @throws IllegalArgumentException if there is a mismatch between the argument and operation
     */
    public QueryExpression(int op, QueryEvaluable arg) throws IllegalArgumentException {
        if (!(op == UPPER || op == LOWER)) {
            throw new IllegalArgumentException("Invalid operation for specified arguments");
        }
        if (arg.getType().equals(UnknownTypeValue.class)) {
            arg.youAreType(String.class);
        } else if (!arg.getType().equals(String.class)) {
            throw new ClassCastException("Invalid argument (" + arg.getType() + ") for "
                    + (op == UPPER ? "UPPER()" : "LOWER()") + " operation");
        }
        arg1 = arg;
        this.op = op;
        type = String.class;
    }
    
    /**
       * {@inheritDoc}
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

    /**
     * {@inheritDoc}
     */
    public void youAreType(Class cls) {
        if (type.equals(UnknownTypeValue.class)) {
            // Must be the numeric operation
            arg1.youAreType(cls);
            arg2.youAreType(cls);
            type = cls;
        } else {
            throw new ClassCastException("youAreType called on QueryExpression that already has "
                    + "type");
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getApproximateType() {
        if (type.equals(UnknownTypeValue.class)) {
            return arg1.getApproximateType();
        } else {
            throw new ClassCastException("getApproximateType called when type is known");
        }
    }

}
