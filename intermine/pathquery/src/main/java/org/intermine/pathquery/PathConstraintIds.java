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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.intermine.metadata.ConstraintOp;

/**
 * Representation of a constraint to a collection of object ids in a query.
 *
 * @author Matthew Wakeling
 */
public class PathConstraintIds extends PathConstraint
{
    /** List of valid ops for this type of constraint */
    public static final Set<ConstraintOp> VALID_OPS = new HashSet<ConstraintOp>(Arrays.asList(
                ConstraintOp.IN, ConstraintOp.NOT_IN));
    private Collection<Integer> ids;

    /**
     * Constructs a new PathConstraintIds. The path should be a normal path expression
     * with dots separating the parts. Do not use colons to represent outer joins, and do not
     * use square brackets to represent subclass constraints. The path will be checked for
     * format, but can only be verified once inside a PathQuery object by the
     * PathQuery.verifyQuery() method. This object is used to form a constraint to a bag.
     *
     * @param path the path that the constraint is attached to
     * @param op the type of operation
     * @param ids the collection of ids to constrain to
     * @throws NullPointerException if path, op, or ids are null
     * @throws IllegalArgumentException if the path contains colons or square brackets, or is
     * otherwise in a bad format, or if the op is invalid for this constraint type
     */
    public PathConstraintIds(String path, ConstraintOp op, Collection<Integer> ids) {
        super(path, op);
        if (op == null) {
            throw new NullPointerException("Cannot construct a PathConstraintIds with a null op");
        }
        if (!VALID_OPS.contains(op)) {
            throw new IllegalArgumentException("PathConstraints to bags must use an op in"
                    + " the list \"" + VALID_OPS + "\"");
        }
        if (ids == null) {
            throw new NullPointerException("Cannot create a constraint to a null bag");
        }
        this.ids = ids;
    }

    /**
     * Returns the id collection that the constraint is constraining to.
     *
     * @return a Collection of Integers
     */
    public Collection<Integer> getIds() {
        return ids;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return path + " " + op + " " + ids;
    }
}
