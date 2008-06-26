package org.intermine.web.logic.query;

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

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.Constraint;

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
//    public static Constraint and(Constraint constraint1, Constraint constraint2) {
//        return null;
//    }


    /**
     * Creates constraint with the logical expression OR
     * @param constraint1 first constraint
     * @param constraint2 second constraint
     * @return the constraint
     */
//    public static Constraint or(Constraint constraint1, Constraint constraint2) {
//        return null;
//    }


    /**
     * Creates constraint with the operator EQUALS

     * @param value value to constrain field to
     * @return the EQUALS constraint
     */
    public static Constraint eq(Object value) {
        return new Constraint(ConstraintOp.EQUALS, value);
    }

    /**
     * Creates constraint with the operator NOT_EQUALS

     * @param value value to constrain field to
     * @return the NOT_EQUALS constraint
     */
    public static Constraint neq(Object value) {
        return new Constraint(ConstraintOp.NOT_EQUALS, value);
    }

    /**
     * Creates constraint with the operator LIKE
     * @param value value of field
     * @return the LIKE constraint
     */
    public static Constraint like(String value) {
        return new Constraint(ConstraintOp.CONTAINS, value);
    }

    /**
     * Creates constraint with the operator CONTAINS
     * @param value value of field
     * @return the CONTAINS constraint
     */
    public static Constraint contains(String value) {
        return new Constraint(ConstraintOp.EQUALS, value);
    }

    /**
     * (I wasn't clear on whether or not we were going to make these available to the API or not)
     * Creates constraint with the operator LOOKUP
     * @param value value of field
     * @return the LOOKUP constraint
     */
    public static Constraint lookup(String value) {
        return new Constraint(ConstraintOp.LOOKUP, value);
    }

    /**
     * Creates constraint with the operator BETWEEN
     * @param start the lower value
     * @param end the upper value
     * @return the BETWEEN constraint
     */
    public static Constraint between(Object start, Object end) {
        return new Constraint(ConstraintOp.BETWEEN, start, end);
    }

    /**
     * Creates constraint with the operator IN
     * @param bag name of bag
     * @return the IN constraint
     */
    public static Constraint in(String bag) {
        return new Constraint(ConstraintOp.IN, bag);
    }


    /**
     * Creates constraint with the operator IN.  validate that list is the same type as the
     * attribute in the model
     * @param values list of values
     * @return the IN constraint
     */
    public static Constraint in(List<?> values) {
        return new Constraint(ConstraintOp.IN, values);
    }


    /**
     * Creates constraint with the operator NOT_IN
     * @param bag name of bag
     * @return the NOT_IN constraint
     */
    public static Constraint notIn(String bag) {
        return new Constraint(ConstraintOp.NOT_IN, bag);
    }


    /**
     * Creates constraint with the operator NOT_IN
     * @param values list of values
     * @return the NOT_IN constraint
     */
    public static Constraint notIn(List<?> values) {
        return new Constraint(ConstraintOp.NOT_IN, values);
    }

    /**
     * @return the NULL constraint
     */
    public static Constraint isNull() {
        return new Constraint(ConstraintOp.IS_NULL);
    }

    /**
     * @return the NOT_NULL constraint
     */
    public static Constraint isNotNull() {
        return new Constraint(ConstraintOp.IS_NOT_NULL);
    }

    /**
     * Creates constraint with the logical expression >

     * @param value value to constrain field to
     * @return the constraint
     */
    public static Constraint greaterThan(Object value) {
        return new Constraint(ConstraintOp.GREATER_THAN, value);
    }

    /**
     * Creates constraint with the logical expression >=

     * @param value value to constrain field to
     * @return the constraint
     */
    public static Constraint greaterThanEqualTo(Object value) {
        return new Constraint(ConstraintOp.GREATER_THAN_EQUALS, value);
    }

    /**
     * Creates constraint with the logical expression <

     * @param value value to constrain field to
     * @return the constraint
     */
    public static Constraint lessThan(Object value) {
        return new Constraint(ConstraintOp.LESS_THAN, value);
    }

    /**
     * Creates constraint with the logical expression <=

     * @param value value to constrain field to
     * @return the constraint
     */
    public static Constraint lessThanEqualTo(Object value) {
        return new Constraint(ConstraintOp.LESS_THAN_EQUALS, value);
    }
}
