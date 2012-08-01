package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;

/**
 * Operations used in building constraints.
 *
 * @author Mark Woodbridge
 * @author Matthew Wakeling
 */
public final class ConstraintOp
{
    private static List<ConstraintOp> values = new ArrayList<ConstraintOp>();
    private final String name;

    /** Require that the two arguments are equal, regardless of case for strings */
    public static final ConstraintOp EQUALS = new ConstraintOp("=");
    /** Require that the two arguments are exactly equal */
    public static final ConstraintOp EXACT_MATCH = new ConstraintOp("==");
    /** Require that the two arguments are not equal, ignoring case for strings */
    public static final ConstraintOp NOT_EQUALS = new ConstraintOp("!=");
    /** Require that the two arguments are not equal */
    public static final ConstraintOp STRICT_NOT_EQUALS = new ConstraintOp("!==");
    /** Require that the first argument is less than the second */
    public static final ConstraintOp LESS_THAN = new ConstraintOp("<");
    /** Require that the first argument is less than or equal to the second */
    public static final ConstraintOp LESS_THAN_EQUALS = new ConstraintOp("<=");
    /** Require that the first argument is greater than the second */
    public static final ConstraintOp GREATER_THAN = new ConstraintOp(">");
    /** Require that the first argument is greater than or equal to the second */
    public static final ConstraintOp GREATER_THAN_EQUALS = new ConstraintOp(">=");
    /** Require that the two arguments match */
    public static final ConstraintOp MATCHES = new ConstraintOp("LIKE");
    /** Require that the two arguments do not match */
    public static final ConstraintOp DOES_NOT_MATCH = new ConstraintOp("NOT LIKE");
    /** Require that the argument is null */
    public static final ConstraintOp IS_NULL = new ConstraintOp("IS NULL");
    /** Require that the argument is not null */
    public static final ConstraintOp IS_NOT_NULL = new ConstraintOp("IS NOT NULL");
    /** Require that the first argument contains the second */
    public static final ConstraintOp CONTAINS = new ConstraintOp("CONTAINS");
    /** Require that the first argument does not contain the second */
    public static final ConstraintOp DOES_NOT_CONTAIN = new ConstraintOp("DOES NOT CONTAIN");
    /** Require that the first argument is IN the second */
    public static final ConstraintOp IN = new ConstraintOp("IN");
    /** Require that the first argument is NOT IN the second */
    public static final ConstraintOp NOT_IN = new ConstraintOp("NOT IN");

    /** Subquery exists */
    public static final ConstraintOp EXISTS = CONTAINS;
    /** Subquery does not exist */
    public static final ConstraintOp DOES_NOT_EXIST = DOES_NOT_CONTAIN;

    /** Combine constraints with the AND operation */
    public static final ConstraintOp AND = new ConstraintOp("AND");
    /** Combine constraints with the OR operation */
    public static final ConstraintOp OR = new ConstraintOp("OR");
    /** Combine constraints with the NAND operation */
    public static final ConstraintOp NAND = new ConstraintOp("NAND");
    /** Combine constraints with the NOR operation */
    public static final ConstraintOp NOR = new ConstraintOp("NOR");

    /** Special operation indicating a bag upload step should be used, for the webapp only. */
    public static final ConstraintOp LOOKUP = new ConstraintOp("LOOKUP");

    /** Require that a range overlaps another range */
    public static final ConstraintOp OVERLAPS = new ConstraintOp("OVERLAPS");
    /** Require that a range does not overlap another range */
    public static final ConstraintOp DOES_NOT_OVERLAP = new ConstraintOp("DOES NOT OVERLAP");

    /** Require that the first argument is one of a list a values */
    public static final ConstraintOp ONE_OF = new ConstraintOp("ONE OF");
    /** Require that the first argument is not one of a list of values */
    public static final ConstraintOp NONE_OF = new ConstraintOp("NONE OF");
    
    /** Require that the first argument lie entirely within the second. **/
	public static final ConstraintOp WITHIN = new ConstraintOp("WITHIN");
	
	/** Require that some part of the first argument lie outside the second. **/
	public static final ConstraintOp OUTSIDE = new ConstraintOp("OUTSIDE");

    private ConstraintOp(String name) {
        this.name = name;
        values.add(this);
    }

    /**
     * Get the String representation of this ConstraintOp
     * @return a String
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Get an index for this ConstraintOp
     * (Only for use in webapp)
     * @return the index
     */
    public Integer getIndex() {
        return new Integer(values.indexOf(this));
    }

    /**
     * Convert an index to a ConstraintOp
     * (Only for use in webapp)
     * @param index the index
     * @return the ConstraintOp
     */
    public static ConstraintOp getOpForIndex(Integer index) {
        return values.get(index.intValue());
    }

    /**
     * Get the the internal list of ConstraintOps
     * (Only for use in webapp)
     * @return the List of ConstraintOps
     */
    public static List<ConstraintOp> getValues() {
        return values;
    }

    /**
     * Get the negated op
     *
     * @return the negated op
     */
    public ConstraintOp negate() {
        if (this == EQUALS) {
            return NOT_EQUALS;
        } else if (this == EXACT_MATCH) {
            return STRICT_NOT_EQUALS;
        } else if (this == NOT_EQUALS) {
            return EQUALS;
        } else if (this == STRICT_NOT_EQUALS) {
            return EXACT_MATCH;
        } else if (this == LESS_THAN) {
            return GREATER_THAN_EQUALS;
        } else if (this == GREATER_THAN_EQUALS) {
            return LESS_THAN;
        } else if (this == GREATER_THAN) {
            return LESS_THAN_EQUALS;
        } else if (this == LESS_THAN_EQUALS) {
            return GREATER_THAN;
        } else if (this == MATCHES) {
            return DOES_NOT_MATCH;
        } else if (this == DOES_NOT_MATCH) {
            return MATCHES;
        } else if (this == IS_NULL) {
            return IS_NOT_NULL;
        } else if (this == IS_NOT_NULL) {
            return IS_NULL;
        } else if (this == CONTAINS) {
            return DOES_NOT_CONTAIN;
        } else if (this == DOES_NOT_CONTAIN) {
            return CONTAINS;
        } else if (this == IN) {
            return NOT_IN;
        } else if (this == NOT_IN) {
            return IN;
        } else if (this == AND) {
            return NAND;
        } else if (this == NAND) {
            return AND;
        } else if (this == OR) {
            return NOR;
        } else if (this == NOR) {
            return OR;
        } else if (this == ONE_OF) {
            return NONE_OF;
        } else if (this == NONE_OF) {
            return ONE_OF;
        } else if (this == WITHIN) {
        	return OUTSIDE;
        } else if (this == OUTSIDE) {
        	return WITHIN;
        }
        throw new IllegalArgumentException("Unknown op");
    }

    /**
     * Get ConstraintOp for given operation code.
     * @param operationCode operation as string
     * @return ConstraintOp if operation code is valid else null
     */
    public static ConstraintOp getConstraintOp(String operationCode) {
        if (operationCode == null) {
            return null;
        }
        String opCode = operationCode.trim().toUpperCase();
        for (ConstraintOp op : values) {
            if (op.getName().equalsIgnoreCase(opCode)) {
                return op;
            }
        }
        return null;
    }

    private String getName() {
        return name;
    }
}
