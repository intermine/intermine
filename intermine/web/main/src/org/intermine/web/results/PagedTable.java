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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * A pageable and configurable table of data.
 *
 * @author Andrew Varley
 * @author Kim Rutherford
 */
public abstract class PagedTable
{
    private static final Logger LOG = Logger.getLogger(PagedTable.class);

    private List columns = new ArrayList();
    private List columnNames = null;
    private List rows;
    private int startRow = 0;
    private int pageSize = 10;
    private boolean summary = true;

    /**
     * Construct a PagedTable with a list of column names
     * @param columnNames the column headings
     */
    public PagedTable(List columns) {
        this.columns = columns;
    }

    /**
     * Construct a PagedTable with a list of column names and a parameter indicating whether this
     * table should display its cells in a summary or detailed format
     * @param columnNames the column headings
     * @param summary the format for displaying cells
     */
    public PagedTable(List columns, boolean summary) {
        this(columns);
        this.summary = summary;
    }

    /**
     * Check the format for displaying cells
     * @return true if this table should display its cells in a summary format
     */
    public boolean isSummary() {
        return summary;
    }

    /**
     * Get the list of column configurations
     *
     * @return the List of columns in the order they are to be displayed
     */
    public List getColumns() {
        return Collections.unmodifiableList(columns);
    }


    /**
     * @return
     */
    public List getColumnNames() {
        if (columnNames == null) {
            columnNames = new ArrayList();
            Iterator iter = columns.iterator();
            while (iter.hasNext()) {
                String columnName = ((Column) iter.next()).getPath().toString();
                columnNames.add(columnName);
            }
        }
        return columnNames;
    }

    /**
     * Return the number of visible columns.  Used by JSP pages.
     * @return the number of visible columns.
     */
    public int getVisibleColumnCount() {
        int count = 0;
        for (Iterator i = columns.iterator(); i.hasNext();) {
            if (((Column) i.next()).isVisible())  {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Move a column left
     *
     * @param index the index of the column to move
     */
    public void moveColumnLeft(int index) {
        if (index > 0 && index <= columns.size() - 1) {
            columns.add(index - 1, columns.remove(index));
        }
    }
    
    /**
     * Move a column right
     *
     * @param index the index of the column to move
     */
    public void moveColumnRight(int index) {
        if (index >= 0 && index < columns.size() - 1) {
            columns.add(index + 1, columns.remove(index));
        }
    }

    /**
     * Set the page size of the table
     *
     * @param pageSize the page size
     * @throws PageOutOfRangeException if the page is out of range for this source collection
     * (ie. is past the offset limit for the objectstore)
     */    
    public void setPageSize(int pageSize) throws PageOutOfRangeException {
        this.pageSize = pageSize;
        startRow = (startRow / pageSize) * pageSize;
        updateRows();
    }

    /**
     * Get the page size of the current page
     *
     * @return the page size
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Get the index of the first row of this page
     * @return the index
     */
    public int getStartRow() {
        return startRow;
    }

    /**
     * Get the page index.
     * @return current page index
     */
    public int getPage() {
        return (startRow / pageSize);
    }
    
    /**
     * Set the page size and page together.
     * 
     * @param page page number
     * @param size page size
     * @throws PageOutOfRangeException if the page is out of range for this source collection
     * (ie. is past the offset limit for the objectstore)
     */
    public void setPageAndPageSize(int page, int size)
        throws PageOutOfRangeException {
        int oldStartRow = this.startRow;
        int oldPageSize = this.pageSize;
        
        try {
            this.pageSize = size;
            this.startRow = size * page;
            updateRows();
        } catch (PageOutOfRangeException e) {
            // reset state
            this.startRow = oldStartRow;
            this.pageSize = oldPageSize;
            throw e;
        }
    }
    
    /**
     * Get the index of the last row of this page
     * @return the index
     */
    public int getEndRow() {
        return startRow + rows.size() - 1;
    }

    /**
     * Go to the first page
     */
    public void firstPage() {
        startRow = 0;
        try {
            updateRows();
        } catch (PageOutOfRangeException e) {
            throw new RuntimeException("failed to go to the first page in PagedTable", e);
        }
    }

    /**
     * Check if were are on the first page
     * @return true if we are on the first page
     */
    public boolean isFirstPage() {
        return (startRow == 0);
    }

    /**
     * Go to the last page
     * @throws PageOutOfRangeException if the page is out of range for this source collection
     * (ie. is past the offset limit for the objectstore)
     */
    public void lastPage() throws PageOutOfRangeException {
        int oldStartRow = startRow;
        try {
            startRow = ((getExactSize() - 1) / pageSize) * pageSize;
            updateRows();
        } catch (PageOutOfRangeException e) {
            // go back to where we were
            startRow = oldStartRow;
            throw e;
        }
    }

    /**
     * Check if we are on the last page
     * @return true if we are on the last page
     */
    public boolean isLastPage() {
        return (!isSizeEstimate() && getEndRow() == getSize() - 1);
    }

    /**
     * Go to the previous page
     */
    public void previousPage() {
        if (startRow >= pageSize) {
            startRow -= pageSize;
        }
        try {
            updateRows();
        } catch (PageOutOfRangeException e) {
            LOG.error("OutOfRangeException exception in PagedTable: " + e.getStackTrace());

            // if we get here a previous call to nextPage() or lastPage() didn't through
            // OutOfRangeException as it should.  return to first page in the hope that that doesn't
            // cause another exception
            firstPage();
        }
    }

    /**
     * Go to the next page
     * @throws PageOutOfRangeException if the page is out of range for this source collection
     * (ie. is past the offset limit for the objectstore)
     */
    public void nextPage() throws PageOutOfRangeException {
        int oldStartRow = startRow;
        try {
            startRow += pageSize;
            updateRows();
        } catch (PageOutOfRangeException e) {
            // go back to where we were
            startRow = oldStartRow;
            throw e;
        }
    }

    /**
     * Return the currently visible rows of the table as a List of Lists.
     *
     * @return the rows of the table
     */    
    public List getRows() {
        return rows;
    }

    /**
     * Set the currently visible rows
     * @param rows the new rows
     */
    protected void setRows(List rows) {
        this.rows = rows;
    }    

    /**
     * Return all the rows of the table as a List of Lists.
     *
     * @return all the rows of the table
     */
    public abstract WebColumnTable getAllRows();

    /**
     * Get the (possibly estimated) number of rows of this table
     * @return the number of rows
     */
    public abstract int getSize();

    /**
     * Check whether the result of getSize is an estimate
     * @return true if the size is an estimate
     */
    public abstract boolean isSizeEstimate();

    /**
     * Get the exact number of rows of this table
     * @return the number of rows
     */
    protected abstract int getExactSize();

    /**
     * Update the internal row list
     * @throws PageOutOfRangeException if update is unable to get the page we want
     */
    protected abstract void updateRows() throws PageOutOfRangeException;

    /**
     * Return the maximum retrievable index for this PagedTable.  This will only ever return less
     * than getExactSize() if the underlying data source has a restriction on the maximum index
     * that can be retrieved.
     * @return the maximum retrieved index
     */
    public abstract int getMaxRetrievableIndex();
}
