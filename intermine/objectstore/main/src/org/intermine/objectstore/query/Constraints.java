package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.List;

/**
 * Builds constraints for the PathQuery
 * @author julie
 */

public class Constraints
{

    /**
     * Creates constraint with the logical expression AND
     * @param constraint1 first constraint
     * @param constraint2 second constraint
     * @return the constraint
     */
    public static Constraints and(Constraints constraint1, Constraints constraint2) {
        return null;
    }


    /**
     * Creates constraint with the logical expression OR
     * @param constraint1 first constraint
     * @param constraint2 second constraint
     * @return the constraint
     */
    public static Constraints or(Constraints constraint1, Constraints constraint2) {
        return null;
    }


    /**
     * Creates constraint with the operator EQUALS
     * @param path field in query
     * @param value value to constrain field to
     * @return the EQUALS constraint
     */
    public static Constraints eq(String path, Object value) {
        return null;
    }

    /**
     * Creates constraint with the operator NOT_EQUALS
     * @param path field in query
     * @param value value to constrain field to
     * @return the NOT_EQUALS constraint
     */
    public static Constraints neq(String path, Object value) {
        return null;
    }

    /**
     * Creates constraint with the operator LIKE
     * @param path field in query to constrain
     * @param value value of field
     * @return the LIKE constraint
     */
    public static Constraints like(String path, String value) {
        return null;
    }

    /**
     * Creates constraint with the operator CONTAINS
     * @param path field in query to constrain
     * @param value value of field
     * @return the CONTAINS constraint
     */
    public static Constraints contains(String path, String value) {
        return null;
    }

    /**
     * (I wasn't clear on whether or not we were going to make these available to the API or not)
     * Creates constraint with the operator LOOKUP
     * @param node the class to constrain
     * @param value value of field
     * @return the LOOKUP constraint
     */
    public static Constraints lookup(String node, String value) {
        return null;
    }

    /**
     * Creates constraint with the operator BETWEEN
     * @param path the field in the query to constrain
     * @param start the lower value
     * @param end the upper value
     * @return the BETWEEN constraint
     */
    public static Constraints between(String path, Object start, Object end) {
        return null;
    }

    /**
     * Creates constraint with the operator IN
     * @param path the field in the query to constrain
     * @param bag name of bag
     * @return the IN constraint
     */
    public static Constraints in(String path, String bag) {
        return null;
    }


    /**
     * Creates constraint with the operator IN
     * @param path the field in the query to constrain
     * @param values list of values
     * @return the IN constraint
     */
    public static Constraints in(String path, List<String> values) {
        return null;
    }


    /**
     * Creates constraint with the operator NOT_IN
     * @param path the field in the query to constrain
     * @param bag name of bag
     * @return the NOT_IN constraint
     */
    public static Constraints notIn(String path, String bag) {
        return null;
    }


    /**
     * Creates constraint with the operator NOT_IN
     * @param path the field in the query to constrain
     * @param values list of values
     * @return the NOT_IN constraint
     */
    public static Constraints notIn(String path, List<String> values) {
        return null;
    }

    /**
     * @param path the field in the query to constrain
     * @return the NULL constraint
     */
    public static Constraints isNull(String path) {
        return null;
    }

    /**
     * @param path the field in the query to constrain
     * @return the NOT_NULL constraint
     */
    public static Constraints isNotNull(String path) {
        return null;
    }

    /**
     * Creates constraint with the logical expression >
     * @param path field in query
     * @param value value to constrain field to
     * @return the constraint
     */
    public static Constraints greaterThan(String path, Object value) {
        return null;
    }

    /**
     * Creates constraint with the logical expression >=
     * @param path field in query
     * @param value value to constrain field to
     * @return the constraint
     */
    public static Constraints greaterThanEqualTo(String path, Object value) {
        return null;
    }

    /**
     * Creates constraint with the logical expression <
     * @param path field in query
     * @param value value to constrain field to
     * @return the constraint
     */
    public static Constraints lessThan(String path, Object value) {
        return null;
    }

    /**
     * Creates constraint with the logical expression <=
     * @param path field in query
     * @param value value to constrain field to
     * @return the constraint
     */
    public static Constraints lessThanEqualTo(String path, Object value) {
        return null;
    }


}
