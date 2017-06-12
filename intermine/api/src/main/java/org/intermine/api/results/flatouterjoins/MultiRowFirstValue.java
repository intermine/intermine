package org.intermine.api.results.flatouterjoins;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.Util;

/**
 * An object representing an entry in a MultiRow - the first instance of this value in a rowspan.
 *
 * @author Matthew Wakeling
 * @param <E> The value type
 */
public class MultiRowFirstValue<E> extends MultiRowValue<E>
{
    private E value;
    private int rowspan;
    private MultiRowLaterValue mrlv;

    /**
     * Constructor.
     *
     * @param value the value in the results
     * @param rowspan the number of rows that this value spans
     */
    public MultiRowFirstValue(E value, int rowspan) {
        this.value = value;
        this.rowspan = rowspan;
        this.mrlv = new MultiRowLaterValue(this);
    }

    /**
     * {@inheritDoc}
     */
    public E getValue() {
        return value;
    }

    /**
     * Returns the rowspan.
     *
     * @return an integer
     */
    public int getRowspan() {
        return rowspan;
    }

    /**
     * Returns the MultiRowLaterValue corresponding to this object.
     *
     * @return a MultiRowLaterValue object
     */
    public MultiRowLaterValue getMrlv() {
        return mrlv;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (o instanceof MultiRowFirstValue) {
            return Util.equals(value, ((MultiRowFirstValue) o).value)
                && (rowspan == ((MultiRowFirstValue) o).rowspan);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return (value == null ? 0 : value.hashCode()) + rowspan;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "MRFV(" + value + ", " + rowspan + ")";
    }
}
