package org.flymine.objectstore.query;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.ArrayList;

/**
 * Operations used in building constraints
 * @author Mark Woodbridge
 */
public class ConstraintOp
{
    private static List values = new ArrayList();
    private final String name; 

    /**
     * Require that the two arguments are equal
     */
    public static final ConstraintOp EQUALS = new ConstraintOp("=");
    /**
     * Require that the two arguments are not equal
     */
    public static final ConstraintOp NOT_EQUALS = new ConstraintOp("!=");
    /**
     * Require that the first argument is less than the second
     */
    public static final ConstraintOp LESS_THAN = new ConstraintOp("<");
    /**
     * Require that the first argument is less than or equal to the second
     */
    public static final ConstraintOp LESS_THAN_EQUALS = new ConstraintOp("<=");
    /**
     * Require that the first argument is greater than the second
     */
    public static final ConstraintOp GREATER_THAN = new ConstraintOp(">");
    /**
     * Require that the first argument is greater than or equal to the second
     */
    public static final ConstraintOp GREATER_THAN_EQUALS = new ConstraintOp(">=");
    /**
     * Require that the two arguments match
     */
    public static final ConstraintOp MATCHES = new ConstraintOp("LIKE");
    /**
     * Require that the two arguments do not match
     */
    public static final ConstraintOp DOES_NOT_MATCH = new ConstraintOp("NOT LIKE");
    /**
     * Require that the argument is null
     */
    public static final ConstraintOp IS_NULL = new ConstraintOp("IS NULL");
    /**
     * Require that the argument is not null
     */
    public static final ConstraintOp IS_NOT_NULL = new ConstraintOp("IS NOT NULL");
    /**
     * Require that the first argument contains the second
     */
    public static final ConstraintOp CONTAINS = new ConstraintOp("CONTAINS");
    /**
     * Require that the first argument does not contain the second
     */
    public static final ConstraintOp DOES_NOT_CONTAIN = new ConstraintOp("DOES NOT CONTAIN");

    private ConstraintOp(String name) { 
        this.name = name;
        values.add(this);
    } 
    
    /**
     * Get the String representation of this ConstraintOp
     * @return a String
     */
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
        return (ConstraintOp) values.get(index.intValue());
    }
}
