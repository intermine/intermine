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

/**
 * A pageable and configurable table created from a Collection.
 *
 * @author Andrew Varley
 * @author Kim Rutherford
 */
public class PagedCollection implements PagedTable
{
    private List columns = new ArrayList();
    private Column column;
    private int startIndex = 0;
    private int pageSize = 10;

    private Collection collection;
    private List collectionAsList = new ArrayList();
    private String name;

    /**
     * Create a new PagedCollection object from the given Collection.
     *
     * @param name the String to use when displaying this collection - used as the column name for
     * the single column of results
     * @param collection the Collection
     */
    public PagedCollection(Collection collection, String name) {
        this.collection = collection;
        this.name = name;

        for (Iterator i = collection.iterator(); i.hasNext();) {
            ArrayList row = new ArrayList();
            row.add(i.next());
            collectionAsList.add(row);
        }

        column = new Column();
        column.setVisible(true);
        column.setName(name);

        columns.add(column);
    }

    /**
     * @see PagedTable#getColumns
     */
    public List getColumns() {
        return columns;
    }

    /**
     * @see PagedTable#getVisibleColumnCount
     */
    public int getVisibleColumnCount() {
        if (column.isVisible()) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * @see PagedTable#getColumnCount
     */
    public int getColumnCount() {
        return 1;
    }

    /**
     * @see PagedTable#moveColumnLeft
     */
    public void moveColumnLeft(int index) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see PagedTable#moveColumnRight
     */   
    public void moveColumnRight(int index) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see PagedTable#getStartIndex
     */
    public int getStartIndex() {
        return startIndex;
    }

    /**
     * @see PagedTable#setStartIndex
     */
    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    /**
     * @see PagedTable#getEndIndex
     */
    public int getEndIndex() {
        int end = startIndex + pageSize - 1;
        if (end + 1 > getSize()) {
            return getSize() - 1;
        } else {
            return end;
        }
    }

    /**
     * @see PagedTable#getPageSize
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * @see PagedTable#setPageSize
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * @see PagedTable#isFirstPage
     */  
    public boolean isFirstPage() {
        return (startIndex == 0);
    }

    /**
     * @see PagedTable#isLastPage
     */ 
    public boolean isLastPage() {
        return (getEndIndex() == getSize() - 1);
    }

    /**
     * @see PagedTable#getList
     */
    public List getList() {
        return collectionAsList;
    }

    /**
     * @see PagedTable#getSize
     */
    public int getSize() {
        return collection.size();
    }

    /**
     * @see PagedTable#isSizeEstimate
     */
    public boolean isSizeEstimate() {
        return false;
    }
}
