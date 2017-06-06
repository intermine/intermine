package org.intermine.pathquery;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;

import org.intermine.metadata.ConstraintOp;

/**
 * Builds constraints for the PathQuery.
 *
 * @author Matthew Wakeling
 */
public final class Constraints
{
    private Constraints() {
    }

    /**
     * Creates a constraint for a path to be equal to a value.
     *
     * @param path the path to apply the constraint to
     * @param value the value to constrain to
     * @return a new PathConstraint object
     */
    public static PathConstraintAttribute eq(String path, String value) {
        return new PathConstraintAttribute(path, ConstraintOp.EQUALS, value);
    }

    /**
     * Creates a constraint for a path to be exactly equal to a value.
     *
     * This does not provide any benefit for numbers, but strings are always lower-cased for
     * equality comparison - this operator means that the user wants to perform strict equals
     * comparison.
     *
     * @param path the path to apply the constraint to
     * @param value the value to constrain to
     * @return a new PathConstraint object
     */
    public static PathConstraintAttribute equalsExactly(String path, String value) {
        return new PathConstraintAttribute(path, ConstraintOp.EXACT_MATCH, value);
    }

    /**
     * Creates a constraint for a path to be not equal to a value.
     *
     * @param path the path to apply the constraint to
     * @param value the value to constrain to
     * @return a new PathConstraint object
     */
    public static PathConstraintAttribute neq(String path, String value) {
        return new PathConstraintAttribute(path, ConstraintOp.NOT_EQUALS, value);
    }

    /**
     * Creates a constraint for a path to be LIKE a value.
     *
     * @param path the path to apply the constraint to
     * @param value the value to constrain to
     * @return a new PathConstraint object
     */
    public static PathConstraintAttribute like(String path, String value) {
        return new PathConstraintAttribute(path, ConstraintOp.MATCHES, value);
    }

    /**
     * Creates a constraint for a path to be NOT LIKE a value.
     *
     * @param path the path to apply the constraint to
     * @param value the value to constrain to
     * @return a new PathConstraint object
     */
    public static PathConstraintAttribute notLike(String path, String value) {
        return new PathConstraintAttribute(path, ConstraintOp.DOES_NOT_MATCH, value);
    }

    /**
     * Creates a constraint for a path to be less than a value.
     *
     * @param path the path to apply the constraint to
     * @param value the value to constrain to
     * @return a new PathConstraint object
     */
    public static PathConstraintAttribute lessThan(String path, String value) {
        return new PathConstraintAttribute(path, ConstraintOp.LESS_THAN, value);
    }

    /**
     * Creates a constraint for a path to be less than or equal to a value.
     *
     * @param path the path to apply the constraint to
     * @param value the value to constrain to
     * @return a new PathConstraint object
     */
    public static PathConstraintAttribute lessThanEqualTo(String path, String value) {
        return new PathConstraintAttribute(path, ConstraintOp.LESS_THAN_EQUALS, value);
    }

    /**
     * Creates a constraint for a path to be greater than a value.
     *
     * @param path the path to apply the constraint to
     * @param value the value to constrain to
     * @return a new PathConstraint object
     */
    public static PathConstraintAttribute greaterThan(String path, String value) {
        return new PathConstraintAttribute(path, ConstraintOp.GREATER_THAN, value);
    }

    /**
     * Creates a constraint for a path to be greater than or equal to a value.
     *
     * @param path the path to apply the constraint to
     * @param value the value to constrain to
     * @return a new PathConstraint object
     */
    public static PathConstraintAttribute greaterThanEqualTo(String path, String value) {
        return new PathConstraintAttribute(path, ConstraintOp.GREATER_THAN_EQUALS, value);
    }

    /**
     * Creates a constraint for a path with the LOOKUP operator.
     *
     * @param path the path to apply the constraint to
     * @param value the value to constrain to
     * @param extraValue the extra value
     * @return a new PathConstraint object
     */
    public static PathConstraintLookup lookup(String path, String value, String extraValue) {
        return new PathConstraintLookup(path, value, extraValue);
    }

    /**
     * Creates a constraint for a path to be IN a named bag.
     *
     * @param path the path to apply the constraint to
     * @param bag the bag name to constrain to
     * @return a new PathConstraint object
     */
    public static PathConstraintBag in(String path, String bag) {
        return new PathConstraintBag(path, ConstraintOp.IN, bag);
    }

    /**
     * Creates a constraint for a path to be NOT IN a named bag.
     *
     * @param path the path to apply the constraint to
     * @param bag the bag name to constrain to
     * @return a new PathConstraint object
     */
    public static PathConstraintBag notIn(String path, String bag) {
        return new PathConstraintBag(path, ConstraintOp.NOT_IN, bag);
    }

    /**
     * Creates a constraint for a path to be IN a collection of ids.
     *
     * @param path the path to apply the constraint to
     * @param ids the Collection of ids to constrain to
     * @return a new PathConstraint object
     */
    public static PathConstraintIds inIds(String path, Collection<Integer> ids) {
        return new PathConstraintIds(path, ConstraintOp.IN, ids);
    }

    /**
     * Creates a constraint for a path to be NOT IN a collection of ids.
     *
     * @param path the path to apply the constraint to
     * @param ids the Collection of ids to constrain to
     * @return a new PathConstraint object
     */
    public static PathConstraintIds notInIds(String path, Collection<Integer> ids) {
        return new PathConstraintIds(path, ConstraintOp.NOT_IN, ids);
    }


    /**
     * Creates a constraint for a path to be ONE OF a collection of values.
     *
     * @param path the path to apply the constraint to
     * @param values the Collection of values to constrain to
     * @return a new PathConstraint object
     */
    public static PathConstraintMultiValue oneOfValues(String path, Collection<String> values) {
        return new PathConstraintMultiValue(path, ConstraintOp.ONE_OF, values);
    }

    /**
     * Creates a constraint for a path to be NOT ONE OF a collection of values.
     *
     * @param path the path to apply the constraint to
     * @param values the Collection of values to constrain to
     * @return a new PathConstraint object
     */
    public static PathConstraintMultiValue noneOfValues(String path, Collection<String> values) {
        return new PathConstraintMultiValue(path, ConstraintOp.NONE_OF, values);
    }

    /**
     * Creates a constraint for a path to be null.
     *
     * @param path the path to apply the constraint to
     * @return a new PathConstraint object
     */
    public static PathConstraintNull isNull(String path) {
        return new PathConstraintNull(path, ConstraintOp.IS_NULL);
    }

    /**
     * Creates a constraint for a path to be not null.
     *
     * @param path the path to apply the constraint to
     * @return a new PathConstraint object
     */
    public static PathConstraintNull isNotNull(String path) {
        return new PathConstraintNull(path, ConstraintOp.IS_NOT_NULL);
    }

    /**
     * Creates a constraint for a path to be a particular subclass (type).  The type should be
     * an unqualified class name that is a valid subclass of the end element of the path.
     *
     * @param path the path to apply the constraint to
     * @param type an unqualified class name of the subclass
     * @return a new PathConstraint object
     */
    public static PathConstraintSubclass type(String path, String type) {
        return new PathConstraintSubclass(path, type);
    }

    /**
     * Creates a constraint for a path to be equal to a loopPath.
     *
     * @param path the path that the constraint is attached to
     * @param loopPath the path that the constraint is looped onto
     * @return a new PathConstraint object
     */
    public static PathConstraintLoop equalToLoop(String path, String loopPath) {
        return new PathConstraintLoop(path, ConstraintOp.EQUALS, loopPath);
    }

    /**
     * Creates a constraint for a path to be not equal to a loopPath.
     *
     * @param path the path that the constraint is attached to
     * @param loopPath the path that the constraint is looped onto
     * @return a new PathConstraint object
     */
    public static PathConstraintLoop notEqualToLoop(String path, String loopPath) {
        return new PathConstraintLoop(path, ConstraintOp.NOT_EQUALS, loopPath);
    }

    /**
     * Creates a constraint for a path to contain a value.
     *
     * @param path the path that the constraint is attached to
     * @param contained the value this path's field should contain.
     * @return a new PathConstraint object
     */
    public static PathConstraintAttribute contains(String path, String contained) {
        return new PathConstraintAttribute(path, ConstraintOp.CONTAINS, contained);
    }

    /**
     * Creates a constraint for a path to not contain a value.
     *
     * @param path the path that the constraint is attached to
     * @param contained the value this path's field should not contain.
     * @return a new PathConstraint object
     */
    public static PathConstraintAttribute doesNotContain(String path, String contained) {
        return new PathConstraintAttribute(path, ConstraintOp.DOES_NOT_CONTAIN, contained);
    }
}
