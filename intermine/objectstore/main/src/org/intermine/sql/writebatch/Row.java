package org.intermine.sql.writebatch;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Stores two ints.
 *
 * @author Matthew Wakeling
 */
public class Row implements Comparable
{
    private int left, right;

    /**
     * Constructor.
     *
     * @param left the left integer
     * @param right the right integer
     */
    public Row(int left, int right) {
        this.left = left;
        this.right = right;
    }

    /**
     * Returns left.
     *
     * @return left
     */
    public int getLeft() {
        return left;
    }

    /**
     * Returns right.
     *
     * @return right
     */
    public int getRight() {
        return right;
    }

    /**
     * @see Object#equals
     */
    public boolean equals(Object o) {
        if ((o instanceof Row) && (((Row) o).left == left) && (((Row) o).right == right)) {
            return true;
        }
        return false;
    }

    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return left + (1013 * right);
    }

    /**
     * @see Comparable#compareTo
     */
    public int compareTo(Object o) {
        Row r = (Row) o;
        int retval = left - r.left;
        if (retval == 0) {
            retval = right - r.right;
        }
        return retval;
    }
}
