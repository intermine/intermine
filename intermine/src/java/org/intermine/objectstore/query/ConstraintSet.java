package org.flymine.objectstore.query;

import java.util.LinkedHashSet;
import java.util.Set;

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
    protected LinkedHashSet constraints;
    protected boolean negated;


    /**
     * Construct empty ConstraintSet setting disjunctive (true = OR)
     *
     * @param disjunctive relationship between constraints (true = OR, false = AND)
     */
    public ConstraintSet(boolean disjunctive) {
        this(disjunctive, false);
    }

    /**
     * Construct empty ConstraintSet setting disjunctive (true = OR), and with boolean negated field
     *
     * @param disjunctive relationship between constraints (true = OR, false = AND)
     * @param negated true to negate the sense of the whole ConstraintSet
     */
    public ConstraintSet(boolean disjunctive, boolean negated) {
        this.disjunctive = disjunctive;
        this.constraints = new LinkedHashSet();
        this.negated = negated;
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

    /**
     * Returns the Set of constraints.
     *
     * @return Set of Constraint objects
     */
    public Set getConstraints() {
        return constraints;
    }
}
