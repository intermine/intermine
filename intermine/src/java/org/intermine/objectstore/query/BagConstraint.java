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

import org.flymine.util.Util;

/**
 * Constraint a QueryClass or QueryEvaluable to be within a bag.
 *
 * @author Matthew Wakeling
 */
public class BagConstraint extends Constraint
{
    protected QueryEvaluable qe;
    protected QueryClass qc;
    protected Set bag;

    /**
     * Construct a BagConstraint with a QueryEvaluable.
     *
     * @param qe the QueryEvaluable to compare to the bag
     * @param bag a Collection that represents the bag
     */
    public BagConstraint(QueryEvaluable qe, Collection bag) {
        this(qe, bag, false);
    }

    /**
     * Construct a BagConstraint with a QueryEvaluable.
     *
     * @param qe the QueryEvaluable to compare to the bag
     * @param bag a Collection that represents the bag
     * @param negated reverse the constraint logic if true
     */
    public BagConstraint(QueryEvaluable qe, Collection bag, boolean negated) {
        if (qe == null) {
            throw new NullPointerException("qe cannot be null");
        }
        if (bag == null) {
            throw new NullPointerException("bag cannot be null");
        }
        this.qe = qe;
        this.qc = null;
        this.bag = new HashSet(bag);
        this.negated = negated;
    }

    /**
     * Construct a BagConstraint with a QueryClass.
     *
     * @param qc the QueryClass to compare to the bag
     * @param bag a Collection that represents the bag
     */
    public BagConstraint(QueryClass qc, Collection bag) {
        this(qc, bag, false);
    }

    /**
     * Construct a BagConstraint with a QueryClass.
     *
     * @param qc the QueryClass to compare to the bag
     * @param bag a Collection that represents the bag
     * @param negated reverse the constraint logic if true
     */
    public BagConstraint(QueryClass qc, Collection bag, boolean negated) {
        if (qc == null) {
            throw new NullPointerException("qc cannot be null");
        }
        if (bag == null) {
            throw new NullPointerException("bag cannot be null");
        }
        this.qe = null;
        this.qc = qc;
        this.bag = new HashSet(bag);
        this.negated = negated;
    }

    /**
     * Get the QueryEvaluable.
     *
     * @return QueryEvaluable
     */
    public QueryEvaluable getQueryEvaluable() {
        return qe;
    }

    /**
     * Get the QueryClass.
     *
     * @return QueryClass
     */
    public QueryClass getQueryClass() {
        return qc;
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
                && Util.equals(qe, bc.qe)
                && Util.equals(qc, bc.qc);
        }
        return false;
    }

    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return bag.hashCode() + (negated ? 3 : 0) + 5 * Util.hashCode(qe) + 7 * Util.hashCode(qc);
    }
}
