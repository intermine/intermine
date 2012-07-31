package org.intermine.pathquery;

import java.util.Collection;

import org.intermine.objectstore.query.ConstraintOp;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


/**
 * Representation of a constraint in the PathQuery. All PathConstraint subclasses must be
 * immutable.
 *
 * @author Matthew Wakeling
 */
public abstract class PathConstraint
{
    protected String path;
    protected ConstraintOp op;

    /**
     * Constructs a new PathConstraint. You will need to call the constructor of a subclass of
     * this abstract class. The path should be a normal path expression with dots separating
     * the parts. Do not use colons to represent outer joins, and do not use square brackets to
     * represent subclass constraints. The path will be checked for format, but can only be
     * verified once inside a PathQuery object by the PathQuery.verifyQuery() method.
     *
     * @param path the path that the constraint is attached to
     * @param op the type of operation
     * @throws NullPointerException if path or op are null
     * @throws IllegalArgumentException if the path contains colons or square brackets, or is
     * otherwise in a bad format
     */
    protected PathConstraint(String path, ConstraintOp op) {
        PathQuery.checkPathFormat(path);
        this.path = path;
        this.op = op;
    }

    /**
     * Returns the path that this constraint is attached to. The path is a normal path
     * expression without colons or square brackets, and may not have been verified against a
     * query.
     *
     * @return a String path
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the operation that this constraint performs.
     *
     * @return a ConstraintOp
     */
    public ConstraintOp getOp() {
        return op;
    }

    /**
     * A static method for accessing values without having to handle
     * the subclasses manually.
     * @param con The constraint.
     * @return A string representing the value.
     */
    public static String getValue(PathConstraint con) {
        if (con instanceof PathConstraintAttribute) {
            return ((PathConstraintAttribute) con).getValue();
        } else if (con instanceof PathConstraintBag) {
            return ((PathConstraintBag) con).getBag();
        } else if (con instanceof PathConstraintLookup) {
            return ((PathConstraintLookup) con).getValue();
        } else if (con instanceof PathConstraintSubclass) {
            return ((PathConstraintSubclass) con).getType();
        } else if (con instanceof PathConstraintLoop) {
            return ((PathConstraintLoop) con).getLoopPath();
        } else if (con instanceof PathConstraintNull) {
            return ((PathConstraintNull) con).getOp().toString();
        }
        return null;
    }

    /**
     * A static method for getting the extra-value if there is one, or
     * null if there is none to be got.
     * @param con The constraint to get the extra value of.
     * @return The constraint's extra-value, or null.
     */
    public static String getExtraValue(PathConstraint con) {
        if (con instanceof PathConstraintLookup) {
            return ((PathConstraintLookup) con).getExtraValue();
        } else {
            return null;
        }
    }

    public static Collection<String> getValues(PathConstraint con) {
        if (con instanceof PathConstraintMultiValue) {
            return ((PathConstraintMultiValue) con).getValues();
        } else {
            return null;
        }
    }

    /**
     * A static method to get the type of this constraint if it is a
     * sub-class constraint, or otherwise return null.
     * @param con The constraint to get the type of.
     * @return The type, or null.
     */
    public static String getType(PathConstraint con) {
        if (con instanceof PathConstraintSubclass) {
            return ((PathConstraintSubclass) con).getType();
        } else {
            return null;
        }
    }
}
