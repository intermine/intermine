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

import org.intermine.objectstore.ObjectStoreException;

import java.util.List;

/**
 * A pageable and configurable table, eg. a Results object
 *
 * @author Andrew Varley
 * @author Kim Rutherford
 */
public interface PagedTable
{
    /**
     * Get the list of column configurations
     *
     * @return the List of columns in the order they are to be displayed
     */
    public List getColumns();

    /**
     * Return the number of visible columns.  Used by JSP pages.
     * @return the number of visible columns.
     */
    public int getVisibleColumnCount();
    
    /**
     * Return the width (number of columns) of the table.  Used by the JSP because
     * getColumns().size() isn't possible in JSTL.
     * @return the table width
     */
    public int getTableWidth();

    /**
     * Move a column left
     *
     * @param index the index of the column to move
     */
    public void moveColumnLeft(int index);
    
    /**
     * Move a column right
     *
     * @param index the index of the column to move
     */
    public void moveColumnRight(int index);

    /**
     * Get the start row of this table
     *
     * @return the start row
     */
    public int getStart();

    /**
     * Set the start row of this table
     *
     * @param start the start row
     */
    public void setStart(int start);

    /**
     * Get the page size of this table
     *
     * @return the page size
     */
    public int getPageSize();

    /**
     * Set the page size of this table
     *
     * @param pageSize the page size
     */
    public void setPageSize(int pageSize);

    /**
     * Get the end row of this table
     *
     * @return the end row
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     */
    public int getEnd() throws ObjectStoreException;

    /**
     * Get the exact size of the underlying object
     * NOTE: this may take a long time
     *
     * @return the size of the underlying object
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     */
    public int getExactSize() throws ObjectStoreException;

    /**
     * Get the approximate size of the underlying object.
     * NOTE: should be fast.
     *
     * @return the size of the underlying object
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     */
    public int getEstimatedSize() throws ObjectStoreException;

    /**
     * Gets whether or not there could be any previous rows
     *
     * @return true if the "previous" button should be shown
     */
    public boolean isPreviousRows();

    /**
     * Gets whether or not there could be more rows
     *
     * @return true if the "next" button should be shown
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     */
    public boolean isMoreRows() throws ObjectStoreException;

    /**
     * Return the rows of the table as a List of Lists.
     *
     * @return the rows of the table
     */
    public List getList();
}
