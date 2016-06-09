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

import java.util.ArrayList;
import java.util.List;

/**
 * Object representing a combination of ObjectStoreBags.
 *
 * @author Matthew Wakeling
 */
public class ObjectStoreBagCombination implements QuerySelectable
{
    // List of either Bags or ObjectStoreBagCombinations
    private final List<QuerySelectable> bags = new ArrayList<QuerySelectable>();
    private final int op;
    /** Constant representing a UNION operation */
    public static final int UNION = 879234;
    /** Constant representing an INTERSECTION operation */
    public static final int INTERSECT = 519552;
    /** Constant representing an EXCEPT operation */
    public static final int EXCEPT = 281056;
    /** Constant representing an all but intersect operation */
    public static final int ALLBUTINTERSECT = 853915;

    /**
     * Constructs a new ObjectStoreBagCombination.
     *
     * @param op the type of combination, out of UNION, INTERSECT, and EXCEPT
     */
    public ObjectStoreBagCombination(int op) {
        if ((op != UNION) && (op != INTERSECT) && (op != EXCEPT) && (op != ALLBUTINTERSECT)) {
            throw new IllegalArgumentException("Illegal type: " + op);
        }
        this.op = op;
    }

    /**
     * Adds a bag to this combination
     *
     * @param bag an ObjectStoreBag
     */
    public void addBag(ObjectStoreBag bag) {
        bags.add(bag);
    }

    /**
     * @param combo a combination of ObjectStoreBags.
     */
    public void addBagCombination(ObjectStoreBagCombination combo) {
        bags.add(combo);
    }

    /**
     * Returns the op of this combination.
     *
     * @return an int
     */
    public int getOp() {
        return op;
    }

    /**
     * Returns the List of bags.
     *
     * @return a List of ObjectStoreBags
     */
    public List<QuerySelectable> getBags() {
        return bags;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getType() {
        return Integer.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof ObjectStoreBagCombination) {
            return bags.equals(((ObjectStoreBagCombination) o).bags)
                && (op == ((ObjectStoreBagCombination) o).op);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return bags.hashCode() + op;
    }
}
