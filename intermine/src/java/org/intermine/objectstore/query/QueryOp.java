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
public class QueryOp
{
    private static List values = new ArrayList();
    private final String name; 

    /**
     * Require that the two arguments are equal
     */
    public static final QueryOp EQUALS = new QueryOp("=");
    /**
     * Require that the two arguments are not equal
     */
    public static final QueryOp NOT_EQUALS = new QueryOp("!=");
    /**
     * Require that the first argument is less than the second
     */
    public static final QueryOp LESS_THAN = new QueryOp("<");
    /**
     * Require that the first argument is less than or equal to the second
     */
    public static final QueryOp LESS_THAN_EQUALS = new QueryOp("<=");
    /**
     * Require that the first argument is greater than the second
     */
    public static final QueryOp GREATER_THAN = new QueryOp(">");
    /**
     * Require that the first argument is greater than or equal to the second
     */
    public static final QueryOp GREATER_THAN_EQUALS = new QueryOp(">=");
    /**
     * Require that the two arguments match
     */
    public static final QueryOp MATCHES = new QueryOp("LIKE");
    /**
     * Require that the two arguments do not match
     */
    public static final QueryOp DOES_NOT_MATCH = new QueryOp("NOT LIKE");
    /**
     * Require that the argument is null
     */
    public static final QueryOp IS_NULL = new QueryOp("IS NULL");
    /**
     * Require that the argument is not null
     */
    public static final QueryOp IS_NOT_NULL = new QueryOp("IS NOT NULL");
    /**
     * Require that the first argument contains the second
     */
    public static final QueryOp CONTAINS = new QueryOp("CONTAINS");
    /**
     * Require that the first argument does not contain the second
     */
    public static final QueryOp DOES_NOT_CONTAIN = new QueryOp("DOES NOT CONTAIN");

    private QueryOp(String name) { 
        this.name = name;
        values.add(this);
    } 
    
    /**
     * Get the String representation of this QueryOp
     * @return a String
     */
    public String toString() {
        return name;
    }
    
    /**
     * Get an index for this QueryOp
     * (Only for use in webapp)
     * @return the index
     */
    public Integer getIndex() {
        return new Integer(values.indexOf(this));
    }

    /**
     * Convert an index to a QueryOp
     * (Only for use in webapp)
     * @param index the index
     * @return the QueryOp
     */
    public static QueryOp getOpForIndex(int index) {
        return (QueryOp) values.get(index);
    }
}
