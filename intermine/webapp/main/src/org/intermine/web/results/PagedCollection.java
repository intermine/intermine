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
import java.util.Collection;
import java.util.Arrays;
import java.util.AbstractList;

/**
 * A pageable and configurable table created from a Collection.
 * @author Mark Woodbridge
 */
public class PagedCollection extends PagedTable
{
    protected List list;

    /**
     * Create a new PagedCollection object from the given Collection.
     *
     * @param name the String to use when displaying this collection - used as the column name for
     * the single column of results
     * @param collection the Collection
     * @param collectionType the class Object of the collection, either a Class object or a
     * ClassDescriptor object
     */
    public PagedCollection(String name, Collection collection, Object collectionType) {
        super(Arrays.asList(new Object[] {name}));
        if (collection instanceof List) {
            list = (List) collection;
        } else {
            list = new ArrayList(collection);
        }
        ((Column) getColumns().get(0)).setType(collectionType);
        updateRows();
    }

    /**
     * @see PagedTable#getAllRows
     */
    public List getAllRows() {
        return new TableAdapter(list);
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
        rows = new TableAdapter(list.subList(startRow, Math.min(startRow + pageSize, list.size())));
    }

    /**
     * @see PagedTable#getMaxRetrievableIndex
     */
    public int getMaxRetrievableIndex() {
        return Integer.MAX_VALUE;
    }

    /**
     * Class to turn a List into a List of Lists of one element
     */
    class TableAdapter extends AbstractList
    {
        List list;
        /**
         * Constructor
         * @param list a List
         */
        TableAdapter(List list) {
            this.list = list;
        }
        /**
         * @see AbstractList#get
         */
        public Object get(int i) {
            ArrayList row = new ArrayList();
            row.add(list.get(i));
            return row;
        }
        /**
         * @see AbstractList#size
         */
        public int size() {
            return list.size();
        }
    }
}
