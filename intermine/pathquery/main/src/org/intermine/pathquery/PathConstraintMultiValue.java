package org.intermine.pathquery;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.intermine.objectstore.query.ConstraintOp;

/**
 * Representation of a constraint to a collection of values in a query.
 *
 * @author Matthew Wakeling
 */
public class PathConstraintMultiValue extends PathConstraint
{
    /** List of valid ops for this type of constraint */
    public static final Set<ConstraintOp> VALID_OPS = new HashSet<ConstraintOp>(Arrays.asList(
                ConstraintOp.ONE_OF, ConstraintOp.NONE_OF));
    
    private Collection<String> values;

    /**
     * Constructs a new PathConstraintMultiValue. The path should be a normal path expression
     * with dots separating the parts. Do not use colons to represent outer joins, and do not
     * use square brackets to represent subclass constraints. The path will be checked for
     * format, but can only be verified once inside a PathQuery object by the
     * PathQuery.verifyQuery() method. This object is used to form a constraint to multiple values.
     *
     * @param path the path that the constraint is attached to
     * @param op the type of operation
     * @param values the collection of values to constrain to
     * @throws NullPointerException if path, op, or ids are null
     * @throws IllegalArgumentException if the path contains colons or square brackets, or is
     * otherwise in a bad format, or if the op is invalid for this constraint type
     */
    public PathConstraintMultiValue(String path, ConstraintOp op, Collection<String> values) {
        super(path, op);
        if (op == null) {
            throw new NullPointerException("Cannot construct a PathConstraintMultiValue with a "
                    + "null op");
        }
        if (!VALID_OPS.contains(op)) {
            throw new IllegalArgumentException("PathConstraints with multiple values must use an op"
                    + " in the list \"" + VALID_OPS + "\"");
        }
        if (values == null) {
            throw new NullPointerException("Cannot create a multivalue constrait with a null "
                    + " collection of values.");
        }
        if (values.isEmpty()) {
        	throw new IllegalArgumentException("at least one value must be supplied.");
        }
        this.values = values;
    }

    /**
     * Returns the value collection that the constraint is constraining to.
     *
     * @return a Collection of Strings
     */
    public Collection<String> getValues() {
        return values;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return path + " " + op + " " + values;
    }
}
