package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Groups a series of Constraints together.  Specify whether in the query
 * the relationship between them should be AND or OR
 *
 * @author Richard Smith
 * @author Mark Woodbridge
 * @author Matthew Wakeling
 */
public class ConstraintSet extends Constraint
{
    protected LinkedHashSet constraints;

    /**
     * Construct empty ConstraintSet
     *
     * @param op relationship between constraints
     */
    public ConstraintSet(ConstraintOp op) {
        if (op == null) {
            throw new NullPointerException("op cannot be null");
        }
        this.op = op;
        this.constraints = new LinkedHashSet();
    }

    /**
     * Add a Constraint to the set
     *
     * @param constraint Constraint to be added to set
     * @return this ConstraintSet
     */
    public ConstraintSet addConstraint(Constraint constraint) {
        constraints.add(constraint);
        return this;
    }

    /**
     * Remove specified constraint
     *
     * @param constraint Constraint to be removed from set
     * @return this ConstraintSet
     */
    public ConstraintSet removeConstraint(Constraint constraint) {
        if (!constraints.contains(constraint)) {
            throw new IllegalArgumentException("Constraint does not exist in set");
        }
        constraints.remove(constraint);
        return this;
    }

    /**
     * Returns the Set of constraints.
     *
     * @return Set of Constraint objects
     */
    public Set getConstraints() {
        return constraints;
    }


    /**
     * Test whether two ConstraintSets are equal, overrides Object.equals().
     * constraints are held in LinkedHashSet, it uses the Set.equals() method
     * which should check for equality regardless of the element ordering imposed
     * in LinkedHashSet.
     *
     * @param obj the object to compare with
     * @return true if objects are equal
     */
    public boolean equals(Object obj) {
        if (obj instanceof ConstraintSet) {
            ConstraintSet cs = (ConstraintSet) obj;
            return (constraints.equals(cs.constraints)
                    && (op == cs.op));
        }
        return false;
    }
    /**
     * Get the hashCode for this object overrides Object.hashCode()
     *
     * @return the hashCode
     */
    public int hashCode() {
        return constraints.hashCode() + 3 * op.hashCode();
    }

    protected static final List VALID_OPS = Arrays.asList(new ConstraintOp[] {ConstraintOp.AND,
        ConstraintOp.OR, ConstraintOp.NAND, ConstraintOp.NOR});
}
