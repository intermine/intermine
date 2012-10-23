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

import org.intermine.objectstore.query.ConstraintOp;

/**
 * Representation of a lookup constraint in the PathQuery
 *
 * @author Matthew Wakeling
 */
public class PathConstraintLookup extends PathConstraint
{
    private String value;
    private String extraValue;

    /**
     * Constructs a new PathConstraintLookup. The path should be a normal path expression
     * with dots separating the parts. Do not use colons to represent outer joins, and do not
     * use square brackets to represent subclass constraints. The path will be checked for
     * format, but can only be verified once inside a PathQuery object by the
     * PathQuery.verifyQuery() method. This object is used to form a lookup constraint.
     *
     * @param path the path that the constraint is attached to
     * @param value the value to look up
     * @param extraValue the extra value (for instance, the organism name)
     * @throws NullPointerException if path or value are null
     * @throws IllegalArgumentException if the path contains colons or square brackets, or is
     * otherwise in a bad format
     */
    public PathConstraintLookup(String path, String value, String extraValue) {
        super(path, ConstraintOp.LOOKUP);
        if (value == null) {
            throw new NullPointerException("Cannot create a lookup constraint with a null value");
        }
        this.value = value;
        this.extraValue = extraValue;
    }

    /**
     * Returns the lookup value for this constraint.
     *
     * @return a String
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the extra value for this constraint.
     *
     * @return a String
     */
    public String getExtraValue() {
        return extraValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return path + " LOOKUP " + value + " IN " + extraValue;
    }
}
