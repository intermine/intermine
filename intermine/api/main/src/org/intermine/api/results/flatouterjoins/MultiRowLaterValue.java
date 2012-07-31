package org.intermine.api.results.flatouterjoins;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * An object representing an entry in a MultiRow - entries other than the first value of the
 * rowspan.
 *
 * @author Matthew Wakeling
 * @param <E> The value type
 */
public class MultiRowLaterValue<E> extends MultiRowValue<E>
{
    private MultiRowFirstValue<E> mrfv;

    /**
     * Constructor - should only really be called from MultiRowFirstValue.
     *
     * @param mrfv a MultiRowFirstValue object
     */
    protected MultiRowLaterValue(MultiRowFirstValue<E> mrfv) {
        this.mrfv = mrfv;
    }

    /**
     * {@inheritDoc}
     */
    public E getValue() {
        return mrfv.getValue();
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (o instanceof MultiRowLaterValue) {
            return mrfv.equals(((MultiRowLaterValue) o).mrfv);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return mrfv.hashCode() - 10;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "MRLV(" + getValue() + ")";
    }
}
