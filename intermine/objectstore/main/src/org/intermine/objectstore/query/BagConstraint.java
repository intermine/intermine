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
import java.util.Collection;
import java.util.List;
import org.intermine.util.Util;

/**
 * Constrain a QueryClass or QueryEvaluable to be within a bag.
 *
 * @author Matthew Wakeling
 */
public class BagConstraint extends Constraint implements ConstraintWithBag
{
    protected QueryNode qn;
    protected Collection<?> bag;
    protected ObjectStoreBag osb;

    /**
     * Construct a BagConstraint from a Collection.  Note that the bag isn't copied so it should
     * not be changed after the Query has been executed.  BagConstraint will accept ONE_OF and
     * NONE_OF as constraint op arguments but will use corresponding IN and NOT_IN.
     *
     * @param qn the QueryNode to compare to the bag
     * @param op the operation
     * @param bag a Collection that represents the bag
     */
    public BagConstraint(QueryNode qn, ConstraintOp op, Collection<?> bag) {
        ConstraintOp translatedOp = getTranslatedOp(op);
        if (qn == null) {
            throw new NullPointerException("qe cannot be null");
        }
        if (translatedOp == null) {
            throw new NullPointerException("op cannot be null");
        }
        if (!VALID_OPS.contains(translatedOp)) {
            throw new IllegalArgumentException("op cannot be " + translatedOp);
        }
        if (bag == null) {
            throw new NullPointerException("bag cannot be null");
        }
        this.qn = qn;
        this.op = translatedOp;
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
        ConstraintOp translatedOp = getTranslatedOp(op);
        if (qn == null) {
            throw new NullPointerException("qe cannot be null");
        }
        if (translatedOp == null) {
            throw new NullPointerException("op cannot be null");
        }
        if (!VALID_OPS.contains(translatedOp)) {
            throw new IllegalArgumentException("op cannot be " + translatedOp);
        }
        if (osb == null) {
            throw new NullPointerException("osb cannot be null");
        }
        this.qn = qn;
        this.op = translatedOp;
        this.osb = osb;
        this.bag = null;
    }

    // translate ONE_OF to IN and NONE_OF to NOT_IN
    private ConstraintOp getTranslatedOp(ConstraintOp op) {
        if (ConstraintOp.ONE_OF.equals(op)) {
            return ConstraintOp.IN;
        } else if (ConstraintOp.NONE_OF.equals(op)) {
            return ConstraintOp.NOT_IN;
        }
        return op;
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
    public Collection<?> getBag() {
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
     * {@inheritDoc}
     */
    @Override
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
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return (bag == null ? osb.hashCode() : bag.hashCode()) + 5 * qn.hashCode();
    }

    /** List of possible operations */
    public static final List<ConstraintOp> VALID_OPS =
        Arrays.asList(new ConstraintOp[] {ConstraintOp.IN, ConstraintOp.NOT_IN});
}
