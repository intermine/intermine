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

import org.intermine.objectstore.ObjectStoreException;

/**
 * A pageable and configurable table created from a Collection.
 *
 * @author Andrew Varley
 * @author Kim Rutherford
 */
public class PagedCollection implements PagedTable
{
    private List columns;
    private int start = 0;
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

        Column column = new Column();
        column.setVisible(true);
        column.setName(name);

        columns = new ArrayList();
        columns.add(column);
    }

    /**
     * Get the list of column configurations
     *
     * @return the List of columns in the order they are to be displayed
     */
    public List getColumns() {
        return columns;
    }

    /**
     * Return the width (number of columns) of the table.  Used by the JSP because
     * getColumns().size() isn't possible in JSTL.
     * @return the table width
     */
    public int getTableWidth() {
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
     * Get the start row of this table
     *
     * @return the start row
     */
    public int getStart() {
        return this.start;
    }

    /**
     * Set the start row of this table
     *
     * @param start the start row
     */
    public void setStart(int start) {
        this.start = start;
    }

    /**
     * Get the page size of this table
     *
     * @return the page size
     */
    public int getPageSize() {
        return this.pageSize;
    }

    /**
     * Set the page size of this table
     *
     * @param pageSize the page size
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Get the end row of this table
     *
     * @return the end row
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     */
    public int getEnd() throws ObjectStoreException {
        int size = getExactSize();
        int end = this.start + this.pageSize - 1;
        if ((end + 1) > size) {
            end = size - 1;
        }
        return end;
    }

    /**
     * Get the underlying collection object
     *
     * @return the underlying collection object
     */
    private Collection getCollection() {
        return collection;
    }

    /**
     * Get the approximate size of the underlying collection object (returns the same as
     * getExactSize() for this implementation of PagedTable)
     *
     * @return the size of the underlying collection object
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     */
    public int getEstimatedSize() throws ObjectStoreException {
        return getCollection().size();
    }

    /**
     * Gets whether or not the size is an estimate (Implementation of the PagedTable interface)
     *
     * @return true if size is an estimate (always returns false for this implementation)
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     */
    public boolean isSizeEstimate() throws ObjectStoreException {
        return false;
    }

    /**
     * Get the exact size of the underlying object.
     *
     * @return the size of the underlying object
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     */
    public int getExactSize() throws ObjectStoreException {
        return getCollection().size();
    }

    /**
     * Gets whether or not there could be any previous rows
     *
     * @return true if the "previous" button should be shown
     */
    public boolean isPreviousRows() {
        return (start > 0);
    }

    /**
     * Gets whether or not there could be more rows
     *
     * @return true if the "next" button should be shown
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     */
    public boolean isMoreRows() throws ObjectStoreException {
        int size = getExactSize();
        if (size == (getEnd() + 1)) {
            return false;
        }
        return true;
    }

    /**
     * Return the rows of the table as a List of Lists.
     *
     * @return the rows of the table
     */
    public List getList() {
        return collectionAsList;
    }
}
