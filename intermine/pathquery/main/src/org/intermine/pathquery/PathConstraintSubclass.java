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

import java.util.Collections;
import java.util.Set;

import org.intermine.metadata.ConstraintOp;

/**
 * Representation of a subclass constraint in the PathQuery. Restricts a path in the query to be
 * an instance of a particular class. Note that this object ALSO adds the path to the query, meaning
 * that the class is joined on, which may affect the number of rows returned in the results.
 *
 * @author Matthew Wakeling
 */
public class PathConstraintSubclass extends PathConstraint
{
    /** List of valid ops for this type of constraint */
    public static final Set<ConstraintOp> VALID_OPS = Collections.singleton(null);
    private String type;

    /**
     * Constructs a new PathConstraintSubclass. The path should be a normal path expression
     * with dots separating the parts. Do not use colons to represent outer joins, and do not
     * use square brackets to represent subclass constraints. The path will be checked for
     * format, but can only be verified once inside a PathQuery object by the
     * PathQuery.verifyQuery() method. This object is used to form a constraint on the class of a
     * path in the query.
     *
     * @param path the path that the constraint is attached to
     * @param type the class to restrict the path to
     * @throws NullPointerException if path or type are null
     * @throws IllegalArgumentException if the path contains colons or square brackets, or is
     * otherwise in a bad format
     */
    public PathConstraintSubclass(String path, String type) {
        super(path, null);
        if (type == null) {
            throw new NullPointerException("Cannot create a subclass constraint with a null type");
        }
        this.type = type;
    }

    /**
     * Returns the class that the constraint is constraining to.
     *
     * @return a String
     */
    public String getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "PathConstraintSubclass(" + path + ", " + type + ")";
    }
}
