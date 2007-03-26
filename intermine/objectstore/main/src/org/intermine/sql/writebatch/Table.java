package org.intermine.sql.writebatch;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * An interface representing all changes to be made to an SQL table of some sort.
 *
 * @author Matthew Wakeling
 */
public interface Table
{
    /**
     * Clears the batch.
     */
    public void clear();

    /**
     * Returns the current amount of data, in bytes, held in this object.
     *
     * @return an int
     */
    public int getSize();
}
