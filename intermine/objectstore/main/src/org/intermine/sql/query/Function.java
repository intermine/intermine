package org.intermine.sql.query;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.util.IdentityMap;

/**
 * A representation of a function in an SQL query.
 *
 * @author Matthew wakeling
 * @author Andrew Varley
 */
public class Function extends AbstractValue
{
    protected int operation;
    protected List<AbstractValue> operands;

    /**
     * COUNT(*) aggregate function - takes no operands.
     */
    public static final int COUNT = 1;
    /**
     * MAX(v) aggregate function - takes one operand.
     */
    public static final int MAX = 2;
    /**
     * MIN(v) aggregate function - takes one operand.
     */
    public static final int MIN = 3;
    /**
     * SUM(v) aggregate function - takes one operand.
     */
    public static final int SUM = 4;
    /**
     * AVG(v) aggregate function - takes one operand.
     */
    public static final int AVG = 5;
    /**
     * PLUS function - takes two or more operands.
     */
    public static final int PLUS = 6;
    /**
     * MINUS function - takes two operands.
     */
    public static final int MINUS = 7;
    /**
     * MULTIPLY function - takes two or more operands.
     */
    public static final int MULTIPLY = 8;
    /**
     * DIVIDE function - takes two operands.
     */
    public static final int DIVIDE = 9;
    /**
     * POWER function - takes two operands.
     */
    public static final int POWER = 10;
    /**
     * MODULO arithmetic function - takes two operands.
     */
    public static final int MODULO = 11;
    /**
     * Type casts - takes two operands.
     */
    public static final int TYPECAST = 12;
    /**
     * STRPOS operator - takes two operands.
     */
    public static final int STRPOS = 13;
    /**
     * SUBSTR operator - takes two or three operands.
     */
    public static final int SUBSTR = 14;
    /**
     * COALESCE operator - used by the precomputed tables' orderby fields.
     */
    public static final int COALESCE = 15;
    /**
     * LOWER operator - takes one operand
     */
    public static final int LOWER = 16;
    /**
     * UPPER operator - takes one operand
     */
    public static final int UPPER = 17;
    /**
     * STDDEV operator - takes one operand
     */
    public static final int STDDEV = 18;
    /**
     * GREATEST(x, y) - takes two operands
     */
    public static final int GREATEST = 19;
    /**
     * LEAST(x, y) - takes two operands
     */
    public static final int LEAST = 20;

    private static final String[] REPRESENTATIONS = {"", "COUNT(*)", "MAX(", "MIN(",
        "SUM(", "AVG(", " + ", " - ", " * ", " / ", " ^ ", " % ", "::", "STRPOS(", "SUBSTR(",
        "COALESCE(", "LOWER(", "UPPER(", "STDDEV(", "GREATEST(", "LEAST("};

    /**
     * Constructor for this Function object.
     *
     * @param operation the operation that this Function represents
     * @throws IllegalArgumentException if operation is not valid
     */
    public Function(int operation) {
        if ((operation < 1) || (operation > 20)) {
            throw (new IllegalArgumentException("operation is not valid"));
        }
        this.operation = operation;
        operands = new ArrayList<AbstractValue>();
    }

    /**
     * Adds an operand to this Function object. Operands are stored in the order they are added.
     *
     * @param obj the AbstractValue to add as an operand
     * @throws IllegalArgumentException if the operation cannot handle that many operands
     */
    public void add(AbstractValue obj) {
        switch (operation) {
            case COUNT:
                throw (new IllegalArgumentException("COUNT does not take any operands"));
            case MAX:
            case MIN:
            case SUM:
            case AVG:
            case LOWER:
            case UPPER:
            case STDDEV:
                if (operands.size() >= 1) {
                    throw (new IllegalArgumentException("This function may only take one operand"));
                }
                break;
            case MODULO:
            case MINUS:
            case DIVIDE:
            case POWER:
            case TYPECAST:
            case STRPOS:
            case COALESCE:
            case GREATEST:
            case LEAST:
                if (operands.size() >= 2) {
                    throw (new IllegalArgumentException("This function may only take"
                                + "two operands"));
                }
                break;
            case SUBSTR:
                if (operands.size() >= 3) {
                    throw new IllegalArgumentException("This function may only take three"
                            + " operands");
                }
                break;
            default:
                // All others are alright
                break;
        }
        operands.add(obj);
    }

    /**
     * Returns a String representation of this Function object, suitable for forming part of an
     * SQL query.
     *
     * @return the String representation
     * @throws IllegalStateException if there aren't the correct number of operands for the
     * operation yet.
     */
    @Override
    public String getSQLString() {
        switch (operation) {
            case COUNT:
                return "COUNT(*)";
            case MAX:
            case MIN:
            case SUM:
            case AVG:
            case LOWER:
            case UPPER:
            case STDDEV:
                if (operands.size() < 1) {
                    throw (new IllegalStateException("This function needs an operand"));
                }
                return REPRESENTATIONS[operation]
                    + operands.get(0).getSQLString() + ")";
            case PLUS:
            case MINUS:
            case MULTIPLY:
            case DIVIDE:
            case POWER:
            case MODULO:
            case TYPECAST:
            case GREATEST:
            case LEAST:
            {
                if (operands.size() < 2) {
                    throw (new IllegalStateException("This function needs two operands"));
                }
                String retval = "";
                if (operation != TYPECAST) {
                    retval += "(";
                }
                boolean needComma = false;
                for (AbstractValue v : operands) {
                    if (needComma) {
                        retval += REPRESENTATIONS[operation];
                    }
                    needComma = true;
                    retval += v.getSQLString();
                }
                if (operation != TYPECAST) {
                    retval += ")";
                }
                return retval;
            }
            case STRPOS:
            case SUBSTR:
            case COALESCE:
            {
                if (operands.size() < 2) {
                    throw (new IllegalStateException("This function needs two operands"));
                }
                String retval = REPRESENTATIONS[operation];
                boolean needComma = false;
                for (AbstractValue v : operands) {
                    if (needComma) {
                        retval += ", ";
                    }
                    needComma = true;
                    retval += v.getSQLString();
                }
                retval += ")";
                return retval;
            }
            default:
                throw new Error("Unrecognised operation " + operation);
        }
    }

    /**
     * Overrides Object.equals.
     *
     * @param obj the Object to compare to
     * @return true if they are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Function) {
            Function objF = (Function) obj;
            if (operation == objF.operation) {
                if ((operation == PLUS) || (operation == MULTIPLY)) {
                    Map<AbstractValue, Integer> a = new HashMap<AbstractValue, Integer>();
                    for (AbstractValue operand : operands) {
                        if (!a.containsKey(operand)) {
                            a.put(operand, new Integer(1));
                        } else {
                            Integer i = a.get(operand);
                            a.put(operand, new Integer(1 + i.intValue()));
                        }
                    }
                    Map<AbstractValue, Integer> b = new HashMap<AbstractValue, Integer>();
                    for (AbstractValue operand : objF.operands) {
                        if (!b.containsKey(operand)) {
                            b.put(operand, new Integer(1));
                        } else {
                            Integer i = b.get(operand);
                            b.put(operand, new Integer(1 + i.intValue()));
                        }
                    }
                    return (a.equals(b));
                } else {
                    return (operands.equals(objF.operands));
                }
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(AbstractValue obj, Map<AbstractTable, AbstractTable> tableMap,
            @SuppressWarnings("unused") Map<AbstractTable, AbstractTable> reverseTableMap) {
        if (tableMap instanceof IdentityMap<?>) {
            return equals(obj) ? EQUAL : INCOMPARABLE;
        }
        return EQUAL;

        //throw new RuntimeException("Not implemented");
/*
        if ((operation == objF.operation) && (operands.size() == objF.operands.size())) {
            Iterator opIter = operands.iterator();
            Iterator oOpIter = objF.operands.iterator();
            while (opIter.hasNext()) {
                AbstractValue a = (AbstractValue) opIter.next();
                AbstractValue b = (AbstractValue) oOpIter.next();
                if (! a.compare(b, tableMap, reverseTableMap)) {
                    return false;
                }
            }
            return true;
        }
        return false;*/
    }

    /**
     * Overrides Object.hashcode.
     *
     * @return an arbitrary integer based on the contents of the Function
     */
    @Override
    public int hashCode() {
        int multiplier = 5;
        int state = operation * 3;
        for (AbstractValue operand : operands) {
            state += multiplier * operand.hashCode();
            if ((operation != PLUS) && (operation != MULTIPLY)) {
                multiplier += 2;
            }
        }
        return state;
    }

    /**
     * Returns the operation of the function.
     *
     * @return operation
     */
    public int getOperation() {
        return operation;
    }

    /**
     * Returns the List of operands of this function.
     *
     * @return all operands in a List
     */
    public List<AbstractValue> getOperands() {
        return operands;
    }

    /**
     * Returns true if this value is an aggregate function.
     *
     * @return a boolean
     */
    @Override
    public boolean isAggregate() {
        switch(operation) {
            case COUNT:
            case MAX:
            case MIN:
            case SUM:
            case AVG:
            case STDDEV:
                return true;
            default:
                for (AbstractValue operand : operands) {
                    if (operand.isAggregate()) {
                        return true;
                    }
                }
                return false;
        }
    }
}
