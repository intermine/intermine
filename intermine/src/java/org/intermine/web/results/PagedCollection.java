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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
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
    private Column column = new Column();
    private List columns;
    private int start = 0;
    private int pageSize = 10;

    private Collection collection;
    private List collectionAsList;
    private String name;

    /**
     * Create a new PagedCollection object from the given Collection.
     *
     * @param name the String to use when displaying this collection - used as the column name for
     * the single column of results
     * @param collection the Collection
     */
    public PagedCollection(Collection collection, String name) {
        this.name = name;
        this.collection = collection;
        // turn the Collection into a List so it is ordered and we can call get(int)
        collectionAsList = new ArrayList();

        Iterator iter = collection.iterator();
        while (iter.hasNext()) {
            Object o = iter.next();
            ArrayList rowList = new ArrayList();
            rowList.add(o);
            collectionAsList.add(rowList);
        }

        column.setName(name);
        column.setIndex(0);

        List newColumns = new LinkedList();
        newColumns.add(column);


        columns = Collections.unmodifiableList(newColumns);
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
     * Get the list of column configurations
     *
     * @param name the name of the column to get
     * @return the column for the given name
     */
    public Column getColumnByName(String name) {
        if (name.equals(this.name)) {
            return column;
        } else {
            throw new IllegalArgumentException("No column with this name: " + name);
        }
    }

    /**
     * Move a column up in the display order
     *
     * @param name the name of the column to move
     */
    public void moveColumnUp(String name) {
        throw new IllegalArgumentException("Can't move this column: " + name);        
    }

    /**
     * Move a column down in the display order
     *
     * @param name the name of the column to move
     */
    public void moveColumnDown(String name) {
        throw new IllegalArgumentException("Can't move this column: " + name);        
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
