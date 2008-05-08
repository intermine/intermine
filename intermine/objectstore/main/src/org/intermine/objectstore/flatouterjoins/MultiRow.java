package org.intermine.objectstore.flatouterjoins;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;

import org.intermine.objectstore.query.ResultsRow;

/**
 * A subclass of ResultsRow returned by the ObjectStoreFlatOuterJoinsImpl indicating a row that
 * can be represented as multiple rows because of collections.
 *
 * @author Matthew Wakeling
 */
public class MultiRow extends ResultsRow
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
    public MultiRow(Collection c) {
        super(c);
    }
}
