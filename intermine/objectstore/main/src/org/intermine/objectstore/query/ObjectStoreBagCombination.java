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

import java.util.ArrayList;
import java.util.List;

/**
 * Object representing a combination of ObjectStoreBags.
 *
 * @author Matthew Wakeling
 */
public class ObjectStoreBagCombination implements QuerySelectable
{
    private final List<ObjectStoreBag> bags = new ArrayList<ObjectStoreBag>();
    private final int op;
    /** Constant representing a UNION operation */
    public static final int UNION = 879234;
    /** Constant representing an INTERSECTION operation */
    public static final int INTERSECT = 519552;
    /** Constant representing an EXCEPT operation */
    public static final int EXCEPT = 281056;

    /**
     * Constructs a new ObjectStoreBagCombination.
     *
     * @param op the type of combination, out of UNION, INTERSECT, and EXCEPT
     */
    public ObjectStoreBagCombination(int op) {
        if ((op != UNION) && (op != INTERSECT) && (op != EXCEPT)) {
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
    public List<ObjectStoreBag> getBags() {
        return bags;
    }

    /**
     * {@inheritDoc}
     */
    public Class getType() {
        return Integer.class;
    }

    /**
     * {@inheritDoc}
     */
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
    public int hashCode() {
        return bags.hashCode() + op;
    }
}
