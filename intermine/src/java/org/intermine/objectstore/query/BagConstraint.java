package org.flymine.objectstore.query;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Constraint a QueryClass or QueryEvaluable to be within a bag.
 *
 * @author Matthew Wakeling
 */
public class BagConstraint extends Constraint
{
    protected QueryNode qn;
    protected Set bag;

    /**
     * Construct a BagConstraint.
     *
     * @param qn the QueryNode to compare to the bag
     * @param bag a Collection that represents the bag
     */
    public BagConstraint(QueryNode qn, Collection bag) {
        this(qn, bag, false);
    }

    /**
     * Construct a BagConstraint.
     *
     * @param qn the QueryNode to compare to the bag
     * @param bag a Collection that represents the bag
     * @param negated reverse the constraint logic if true
     */
    public BagConstraint(QueryNode qn, Collection bag, boolean negated) {
        if (qn == null) {
            throw new NullPointerException("qe cannot be null");
        }
        if (bag == null) {
            throw new NullPointerException("bag cannot be null");
        }
        this.qn = qn;
        this.bag = new HashSet(bag);
        this.negated = negated;
    }

    /**
     * Get the QueryNode.
     *
     * @return QueryNode
     */
    public QueryNode getQueryNode() {
        return qn;
    }

    /**
     * Get the bag.
     *
     * @return a Set of objects in the bag
     */
    public Set getBag() {
        return bag;
    }

    /**
     * @see Object#equals
     */
    public boolean equals(Object obj) {
        if (obj instanceof BagConstraint) {
            BagConstraint bc = (BagConstraint) obj;
            return bag.equals(bc.bag)
                && negated == bc.negated
                && qn.equals(bc.qn);
        }
        return false;
    }

    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return bag.hashCode() + (negated ? 3 : 0) + 5 * qn.hashCode();
    }
}
