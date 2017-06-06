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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.intermine.metadata.ConstraintOp;

/**
 * Representation of a null attribute constraint in the PathQuery
 *
 * @author Matthew Wakeling
 */
public class PathConstraintNull extends PathConstraint
{
    /** List of valid ops for this type of constraint */
    public static final Set<ConstraintOp> VALID_OPS = new HashSet<ConstraintOp>(Arrays.asList(
                ConstraintOp.IS_NULL, ConstraintOp.IS_NOT_NULL));

    /**
     * Constructs a new PathConstraintNull. The path should be a normal path expression
     * with dots separating the parts. Do not use colons to represent outer joins, and do not
     * use square brackets to represent subclass constraints. The path will be checked for
     * format, but can only be verified once inside a PathQuery object by the
     * PathQuery.verifyQuery() method. This object is used to form an is_null or is_not_null
     * constraint on an attribute.
     *
     * @param path the path that the constraint is attached to
     * @param op the type of operation
     * @throws NullPointerException if path or op are null
     * @throws IllegalArgumentException if the path contains colons or square brackets, or is
     * otherwise in a bad format, or if the op is invalid for this constraint type
     */
    public PathConstraintNull(String path, ConstraintOp op) {
        super(path, op);
        if (op == null) {
            throw new NullPointerException("Cannot construct a PathConstraintNull with a null op");
        }
        if (!VALID_OPS.contains(op)) {
            throw new IllegalArgumentException("PathConstraintsNull must use an op in the list \""
                    + VALID_OPS + "\"");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return path + " " + op;
    }
}
