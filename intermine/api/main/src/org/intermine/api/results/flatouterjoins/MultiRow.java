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

import java.util.ArrayList;
import java.util.Collection;

/**
 * A subclass of ArrayList returned by the ResultsFlatOuterJoinsImpl indicating a row that
 * can be represented as multiple rows because of collections.
 *
 * @author Matthew Wakeling
 * @param <E> The element type - usually ResultsRow
 */
public class MultiRow<E> extends ArrayList<E>
{
    /**
     * @see ArrayList#ArrayList
     */
    public MultiRow() {
        super();
    }

    /**
     * @see ArrayList#ArrayList(Collection)
     *
     * @param c an existing Collection
     */
    public MultiRow(Collection<? extends E> c) {
        super(c);
    }
}
