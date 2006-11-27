package org.intermine.web.results;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A pageable and configurable table created from an Object
 * @author Mark Woodbridge
 */
public class PagedObject extends PagedTable
{
    /**
     * Create a new PagedObject from the given Object
     *
     * @param name column heading for this Object
     * @param o the Object
     */
    public PagedObject(String name, Object o) {
        super(Arrays.asList(new Object[] {name}), false);

        List row = new ArrayList();
        row.add(o);
        List newRows = new ArrayList();
        newRows.add(row);
        setRows(newRows);

        ((Column) getColumns().get(0)).setType("java.lang.Object");
    }

    /**
     * @see PagedTable#getAllRows
     */
    public List getAllRows() {
        return getRows();
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
        return 1;
    }

    /**
     * @see PagedTable#updateRows
     */
    protected void updateRows() {
    }

    /**
     * @see PagedTable#getMaxRetrievableIndex
     */
    public int getMaxRetrievableIndex() {
        return 1;
    }
}
