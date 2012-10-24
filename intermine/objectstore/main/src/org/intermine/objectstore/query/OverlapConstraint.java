package org.intermine.objectstore.query;

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
import java.util.List;

/**
 * Represents a constraint comparing two range values on an object.
 *
 * @author Matthew Wakeling
 */
public class OverlapConstraint extends Constraint
{
    protected OverlapRange left, right;
    protected List<ConstraintOp> validOps = Arrays.asList(ConstraintOp.CONTAINS,
            ConstraintOp.DOES_NOT_CONTAIN, ConstraintOp.IN, ConstraintOp.NOT_IN,
            ConstraintOp.OVERLAPS, ConstraintOp.DOES_NOT_OVERLAP);

    /**
     * Construct a constraint.
     *
     * @param left the left range
     * @param op the comparison operation
     * @param right the right range
     * @throws IllegalArgumentException if the comparison op is not valid
     */
    public OverlapConstraint(OverlapRange left, ConstraintOp op, OverlapRange right) {
        if (left == null) {
            throw new NullPointerException("left argument cannot be null");
        }
        if (right == null) {
            throw new NullPointerException("right argument cannot be null");
        }
        if (!validOps.contains(op)) {
            throw new IllegalArgumentException("Invalid constraint " + op
                    + " for overlap constraint");
        }
        this.left = left;
        this.right = right;
        this.op = op;
    }

    /**
     * Returns the left OverlapRange.
     *
     * @return an OverlapRange object
     */
    public OverlapRange getLeft() {
        return left;
    }

    /**
     * Returns the right OverlapRange.
     *
     * @return an OverlapRange object
     */
    public OverlapRange getRight() {
        return right;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof OverlapConstraint) {
            OverlapConstraint oc = (OverlapConstraint) obj;
            return oc.left.equals(left) && oc.op == op && oc.right.equals(right);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return left.hashCode() + 5 * op.hashCode() + 7 * right.hashCode();
    }
}
