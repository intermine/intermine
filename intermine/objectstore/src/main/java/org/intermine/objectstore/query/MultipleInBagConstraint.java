package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.List;

import org.intermine.metadata.ConstraintOp;

/**
 * Constraint type requiring any of the given QueryEvaluables to be in a given bag.
 *
 * @author Matthew Wakeling
 */
public class MultipleInBagConstraint extends Constraint implements ConstraintWithBag
{
    private Collection<?> bag;
    private List<? extends QueryEvaluable> evaluables;

    /**
     * Constructor for this class. Create a constraint that selects rows that have any of the
     * evaluables contained in the bag. This is the same as putting several BagConstraint objects
     * in an OR ConstraintSet, but this will execute faster.
     *
     * @param bag a Collection of values
     * @param evaluables a List of QueryEvaluable objects
     */
    public MultipleInBagConstraint(Collection<?> bag, List<? extends QueryEvaluable> evaluables) {
        this.op = ConstraintOp.IN;
        this.bag = bag;
        this.evaluables = evaluables;
    }

    /**
     * Returns the bag that this object was constructed with.
     *
     * @return a Collection of values
     */
    public Collection<?> getBag() {
        return bag;
    }

    /**
     * Returns the evaluables that this object was constructed with.
     *
     * @return a List of QueryEvaluable objects
     */
    public List<? extends QueryEvaluable> getEvaluables() {
        return evaluables;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof MultipleInBagConstraint) {
            MultipleInBagConstraint mibc = (MultipleInBagConstraint) o;
            return bag.equals(mibc.bag) && evaluables.equals(mibc.evaluables);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return 3 * bag.hashCode() + 5 * evaluables.hashCode();
    }
}
