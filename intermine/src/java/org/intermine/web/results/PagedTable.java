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
import java.util.Collections;
import java.util.Iterator;

/**
 * A pageable and configurable table of data.
 *
 * @author Andrew Varley
 * @author Kim Rutherford
 */
public abstract class PagedTable
{
    protected List columns = new ArrayList();
    protected List rows;
    protected int startRow = 0;
    protected int pageSize = 10;
    protected boolean summary = true;

    /**
     * Construct a PagedTable with a list of column names
     * @param columnNames the column headings
     */
    public PagedTable(List columnNames) {
        for (int i = 0; i < columnNames.size(); i++) {
            Column column = new Column();
            column.setName((String) columnNames.get(i));
            column.setIndex(i);
            column.setVisible(true);
            columns.add(column);
        }
    }

    /**
     * Construct a PagedTable with a list of column names and a parameter indicating whether this
     * table should display its cells in a summary or detailed format
     * @param columnNames the column headings
     * @param summary the format for displaying cells
     */
    public PagedTable(List columnNames, boolean summary) {
        this(columnNames);
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
     */    
    public void setPageSize(int pageSize) {
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
        updateRows();
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
     */
    public void lastPage() {
        startRow = ((getExactSize() - 1) / pageSize) * pageSize;
        updateRows();
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
        updateRows();
    }

    /**
     * Go to the next page
     */
    public void nextPage() {
        startRow += pageSize;
        updateRows();
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
     * Return all the rows of the table as a List of Lists.
     *
     * @return all the rows of the table
     */
    public abstract List getAllRows();

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
     */
    protected abstract void updateRows();

    /**
     * Return the maximum retrievable index for this PagedTable.  This will only ever return less
     * than getExactSize() if the underlying data source has a restriction on the maximum index
     * that can be retrieved.
     * @return the maximum retrieved index
     */
    public abstract int getMaxRetrievableIndex();
}
