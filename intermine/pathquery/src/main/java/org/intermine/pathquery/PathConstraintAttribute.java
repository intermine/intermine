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
 * Representation of a normal attribute constraint in the PathQuery
 *
 * @author Matthew Wakeling
 */
public class PathConstraintAttribute extends PathConstraint
{
    /** List of valid ops for this type of constraint */
    public static final Set<ConstraintOp> VALID_OPS = new HashSet<ConstraintOp>(Arrays.asList(
                ConstraintOp.DOES_NOT_MATCH, ConstraintOp.EQUALS, ConstraintOp.GREATER_THAN,
                ConstraintOp.GREATER_THAN_EQUALS, ConstraintOp.LESS_THAN,
                ConstraintOp.LESS_THAN_EQUALS, ConstraintOp.MATCHES, ConstraintOp.NOT_EQUALS,
                ConstraintOp.CONTAINS, ConstraintOp.DOES_NOT_CONTAIN, ConstraintOp.EXACT_MATCH,
                ConstraintOp.STRICT_NOT_EQUALS));
    private String value;

    /**
     * Constructs a new PathConstraintAttribute. The path should be a normal path expression
     * with dots separating the parts. Do not use colons to represent outer joins, and do not
     * use square brackets to represent subclass constraints. The path will be checked for
     * format, but can only be verified once inside a PathQuery object by the
     * PathQuery.verifyQuery() method. This object is used to form a constraint on an attribute.
     *
     * @param path the path that the constraint is attached to
     * @param op the type of operation
     * @param value the value to constrain to, as a String - it will be validated later when the
     * query is validated, as we do not know the attribute type at this stage
     * @throws NullPointerException if path, op, or value are null
     * @throws IllegalArgumentException if the path contains colons or square brackets, or is
     * otherwise in a bad format, or if the op is invalid for this constraint type
     */
    public PathConstraintAttribute(String path, ConstraintOp op, String value) {
        super(path, op);
        if (op == null) {
            throw new NullPointerException("Cannot construct a PathConstraintAttribute with a null"
                    + " op");
        }
        if (!VALID_OPS.contains(op)) {
            throw new IllegalArgumentException("PathConstraints on attributes must use an op in"
                    + " the list \"" + VALID_OPS + "\"");
        }
        if (value == null) {
            throw new NullPointerException("Cannot create a constraint on a null value");
        }
        this.value = value;
    }

    /**
     * Returns the value that the constraint is constraining to.
     *
     * @return a String
     */
    public String getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return path + " " + op + " " + value;
    }
}
