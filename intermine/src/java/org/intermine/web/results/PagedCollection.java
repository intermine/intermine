package org.intermine.web.results;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;

/**
 * A pageable and configurable table created from a Collection.
 *
 * @author Andrew Varley
 * @author Kim Rutherford
 */
public class PagedCollection extends PagedTable
{
    protected List list = new ArrayList();

    /**
     * Create a new PagedCollection object from the given Collection.
     *
     * @param name the String to use when displaying this collection - used as the column name for
     * the single column of results
     * @param collection the Collection
     */
    public PagedCollection(String name, Collection collection) {
        super(Arrays.asList(new Object[] {name}));

        for (Iterator i = collection.iterator(); i.hasNext();) {
            ArrayList row = new ArrayList();
            row.add(i.next());
            list.add(row);
        }

        updateRows();
    }

    /**
     * @see PagedTable#getAllRows
     */
    public List getAllRows() {
        return list;
    }

    /**
     * @see PagedTable#getSize
     */
    public int getSize() {
        return getExactSize();
    }

    /**
     * @see PagedTable#isSizeEstimate
     */
    public boolean isSizeEstimate() {
        return false;
    }

    /**
     * @see PagedTable#getExactSize
     */
    protected int getExactSize() {
        return list.size();
    }

    /**
     * @see PagedTable#updateRows
     */
    protected void updateRows() {
        rows = list.subList(startRow, Math.min(startRow + pageSize, list.size()));
    }
}
