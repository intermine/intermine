package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.intermine.util.Util;

/**
 * Constrain a QueryClass or QueryEvaluable to be within a bag.
 *
 * @author Matthew Wakeling
 */
public class BagConstraint extends Constraint
{
    protected QueryNode qn;
    protected Collection bag;
    protected ObjectStoreBag osb;

    /**
     * Construct a BagConstraint from a Collection.  Note that the bag isn't copied so it should
     * not be changed after the Query has been executed.
     *
     * @param qn the QueryNode to compare to the bag
     * @param op the operation
     * @param bag a Collection that represents the bag
     */
    public BagConstraint(QueryNode qn, ConstraintOp op, Collection bag) {
        if (qn == null) {
            throw new NullPointerException("qe cannot be null");
        }
        if (op == null) {
            throw new NullPointerException("op cannot be null");
        }
        if (!VALID_OPS.contains(op)) {
            throw new IllegalArgumentException("op cannot be " + op);
        }
        if (bag == null) {
            throw new NullPointerException("bag cannot be null");
        }
        this.qn = qn;
        this.op = op;
        this.bag = bag;
        this.osb = null;
    }

    /**
     * Construct a BagConstraint from an ObjectStoreBag.
     *
     * @param qn the QueryNode to compare to the bag
     * @param op the operation
     * @param osb an ObjectStoreBag
     */
    public BagConstraint(QueryNode qn, ConstraintOp op, ObjectStoreBag osb) {
        if (qn == null) {
            throw new NullPointerException("qe cannot be null");
        }
        if (op == null) {
            throw new NullPointerException("op cannot be null");
        }
        if (!VALID_OPS.contains(op)) {
            throw new IllegalArgumentException("op cannot be " + op);
        }
        if (osb == null) {
            throw new NullPointerException("osb cannot be null");
        }
        this.qn = qn;
        this.op = op;
        this.osb = osb;
        this.bag = null;
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
     * Get the bag Collection.
     *
     * @return a Set of objects in the bag
     */
    public Collection getBag() {
        return bag;
    }

    /**
     * Get the ObjectStoreBag.
     *
     * @return an ObjectStoreBag
     */
    public ObjectStoreBag getOsb() {
        return osb;
    }

    /**
     * @see Object#equals
     */
    public boolean equals(Object obj) {
        if (obj instanceof BagConstraint) {
            BagConstraint bc = (BagConstraint) obj;
            return Util.equals(bag, bc.bag)
                && qn.equals(bc.qn)
                && Util.equals(osb, bc.osb);
        }
        return false;
    }

    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return (bag == null ? osb.hashCode() : bag.hashCode()) + 5 * qn.hashCode();
    }
    
    /** List of possible operations */
    public static final List VALID_OPS = Arrays.asList(new ConstraintOp[] {ConstraintOp.IN,
        ConstraintOp.NOT_IN});
}
