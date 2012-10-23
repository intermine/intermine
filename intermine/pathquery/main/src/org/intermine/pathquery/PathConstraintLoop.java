package org.intermine.pathquery;

/*
 * Copyright (C) 2002-2012 FlyMine
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

import org.intermine.objectstore.query.ConstraintOp;

/**
 * Representation of a loop constraint in the PathQuery
 *
 * @author Matthew Wakeling
 */
public class PathConstraintLoop extends PathConstraint
{
    /** List of valid ops for this type of constraint */
    public static final Set<ConstraintOp> VALID_OPS = new HashSet<ConstraintOp>(Arrays.asList(
                ConstraintOp.EQUALS, ConstraintOp.NOT_EQUALS));
    private String loopPath;

    /**
     * Constructs a new PathConstraintLoop from one path to another. The paths should be normal
     * path expressions with dots separating the parts. Do not use colons to represent outer joins,
     * and do not use square brackets to represent subclass constraints. The paths will be checked
     * for format, but can only be verified once inside a PathQuery object by the
     * PathQuery.verifyQuery() method. This object is used to form a loop constraint from one path
     * to another.
     *
     * @param path the path that the constraint is attached to
     * @param op the type of operation
     * @param loopPath the path that the constraint is looped onto
     * @throws NullPointerException if path, op, or loopPath are null
     * @throws IllegalArgumentException if the paths contain colons or square brackets, or are
     * otherwise in a bad format, or if the op is invalid for this constraint type
     */
    public PathConstraintLoop(String path, ConstraintOp op, String loopPath) {
        super(path, op);
        if (op == null) {
            throw new NullPointerException("Cannot construct a PathConstraintLoop with a null"
                    + " op");
        }
        if (!VALID_OPS.contains(op)) {
            throw new IllegalArgumentException("PathConstraints for loops must use an op in"
                    + " the list \"" + VALID_OPS + "\"");
        }
        if (loopPath == null) {
            throw new NullPointerException("Cannot create a loop constraint to a null path");
        }
        PathQuery.checkPathFormat(loopPath);
        this.loopPath = loopPath;
    }

    /**
     * Returns the path that the constraint is constraining to.
     *
     * @return a String
     */
    public String getLoopPath() {
        return loopPath;
    }

    /**
     * Returns a descriptive string for this constraint, mainly useful for checking uniqueness.
     *
     * @return a String
     */
    public String getDescriptiveString() {
        return path.compareTo(loopPath) > 0 ? loopPath + " -- " + path : path + " -- " + loopPath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return path + " " + op + " " + loopPath;
    }
}
