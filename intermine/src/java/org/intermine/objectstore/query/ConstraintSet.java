package org.flymine.objectstore.query;

import java.util.HashSet;

/**
 * Groups a series of Constraints together.  Specify whether in the query
 * the relationship between them should be AND or OR
 *
 * @author Richard Smith
 * @author Mark Woddbridge
 */

public class ConstraintSet implements Constraint
{

    /**
     * All Constraints in set are ANDed together
     */
    public static final boolean AND = false;

    /**
     * All Constrainsts in set are ORed together
     */
    public static final boolean OR = true;

    protected boolean disjunctive;
    protected HashSet constraints;
    protected boolean negated;


    /**
     * Construct empty ConstraintSet setting disjunctive (true = OR)
     *
     * @param disjunctive relationship between constraints (true = OR, false = AND)
     */
    public ConstraintSet(boolean disjunctive) {
        this.disjunctive = disjunctive;
        this.constraints = new HashSet();
        this.negated = false;
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
            throw (new IllegalArgumentException("Constraint does not exist in set"));
        }
        constraints.remove(constraint);
        return this;
    }

    /**
     * Set relationship between Constraints (true = OR, false = AND)
     *
     * @param disjunctive true = OR, false = AND
     */
    public void setDisjunctive(boolean disjunctive) {
        this.disjunctive = disjunctive;
    }

    /**
     * Get relationship between Constraints (true = OR, false = AND)
     *
     * @return true if disjunctive
     */
    public boolean getDisjunctive() {
        return disjunctive;
    }

    /**
     * @see Constraint#setNegated
     */
    public void setNegated(boolean negated) {
        this.negated = negated;
    }

    /**
     * @see Constraint#isNegated
     */
    public boolean isNegated() {
        return negated;
    }
}
