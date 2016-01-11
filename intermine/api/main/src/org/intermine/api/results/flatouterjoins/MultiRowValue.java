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

/**
 * An object representing an entry in a MultiRow.
 *
 * @author Matthew Wakeling
 * @param <E> The value type
 */
public abstract class MultiRowValue<E>
{
    /**
     * Returns the value of the entry.
     *
     * @return an object
     */
    public abstract E getValue();
}
