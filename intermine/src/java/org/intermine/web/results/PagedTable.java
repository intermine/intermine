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
    public int getColumnCount();

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
     * Get the start row of the current page
     *
     * @return the start row
     */
    public int getStartIndex();

    /**
     * Set the start row of the current page
     *
     * @param startIndex the start row
     */
    public void setStartIndex(int startIndex);

    /**
     * Get the end row of the current page
     *
     * @return the end row
     */
    public int getEndIndex();

    /**
     * Get the page size of the current page
     *
     * @return the page size
     */
    public int getPageSize();

    /**
     * Set the page size of the table
     *
     * @param pageSize the page size
     */
    public void setPageSize(int pageSize);

    /**
     * Gets whether or not there could be any previous rows
     *
     * @return true if the "previous" button should be shown
     */
    public boolean isFirstPage();

    /**
     * Gets whether or not there could be more rows
     *
     * @return true if the "next" button should be shown
     */
    public boolean isLastPage();

    /**
     * Return the rows of the table as a List of Lists.
     *
     * @return the rows of the table
     */
    public List getList();

    /**
     * Get the (possibly estimated) number of rows of this table
     * @return the number of rows
     */
    public int getSize();

    /**
     * Check whether the result of getSize is an estimate
     * @return true if the size is an estimate
     */
    public boolean isSizeEstimate();
}
