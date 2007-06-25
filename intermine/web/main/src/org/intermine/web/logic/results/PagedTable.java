package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
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
public class PagedTable
{
    private static final Logger LOG = Logger.getLogger(PagedTable.class);

    private WebTable webTable;
    private List<String> columnNames = null;
    private List<List<ResultElement>> resultElementRows = null;
    private int startRow = 0;
    private int pageSize = 10;

    private List<Column> columns;

    private List<List<Object>> rows = null;

    /**
     * Construct a PagedTable with a list of column names
     * @param webTable the WebTable that this PagedTable will display
     */
    public PagedTable(WebTable webTable) {
        super();
        this.webTable = webTable;
    }

    /**
     * Get the list of column configurations
     *
     * @return the List of columns in the order they are to be displayed
     */
    public List<Column> getColumns() {
        return Collections.unmodifiableList(getColumnsInternal());
    }

    private List<Column> getColumnsInternal() {
        if (columns == null) {
            columns = webTable.getColumns();
        }
        return columns;
    }

    /**
     * Return the column names
     * @return the column names
     */
    public List<String> getColumnNames() {
        if (columnNames == null) {
            columnNames = new ArrayList<String>();
            Iterator iter = getColumns().iterator();
            while (iter.hasNext()) {
                String columnName = ((Column) iter.next()).getName();
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
        for (Iterator<Column> i = getColumnsInternal().iterator(); i.hasNext();) {
            Column obj = i.next();
            if (obj.isVisible()) {
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
        if (index > 0 && index <= getColumnsInternal().size() - 1) {
            getColumnsInternal().add(index - 1, getColumnsInternal().remove(index));
        }
    }
    
    /**
     * Move a column right
     *
     * @param index the index of the column to move
     */
    public void moveColumnRight(int index) {
        if (index >= 0 && index < getColumnsInternal().size() - 1) {
            getColumnsInternal().add(index + 1, getColumnsInternal().remove(index));
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
        updateResultElementRows();
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
            updateResultElementRows();
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
        return startRow + getResultElementRows().size() - 1;
    }

    /**
     * Go to the first page
     */
    public void firstPage() {
        startRow = 0;
        try {
            updateResultElementRows();
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
            updateResultElementRows();
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
            updateResultElementRows();
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
            updateResultElementRows();
        } catch (PageOutOfRangeException e) {
            // go back to where we were
            startRow = oldStartRow;
            throw e;
        }
    }

    /**
     * Return the currently visible rows of the table as a List of Lists of ResultElement objects.
     * @return the resultElementRows of the table
     */    
    public List<List<Object>> getRows() {
        if (rows == null) {
            updateRows();
        }
        return rows;
    }

    /**
     * Return the currently visible rows of the table as a List of Lists of raw values/Objects.
     * @return the ResultElement of the table as rows
     */    
    public List<List<ResultElement>> getResultElementRows() {
        if (resultElementRows == null) {
            try {
                updateResultElementRows();
            } catch (PageOutOfRangeException e) {
                throw new RuntimeException("unexpected exception while getting rows", e);
            }
        }
        return resultElementRows;
    }

    /**
     * Return all the resultElementRows of the table as a List of Lists.
     *
     * @return all the resultElementRows of the table
     */
    public WebTable getAllRows() {
        return webTable;
    }

    /**
     * Temporary hack to let table.jsp get the type of the WebTable.
     * @return the Class of the WebTable for this PagedTable
     */
    public Class<? extends WebTable> getWebTableClass() {
        return webTable.getClass();
    }
    
    /**
     * Get the (possibly estimated) number of resultElementRows of this table
     * @return the number of resultElementRows
     */
    public int getSize() {
        return webTable.size();
    }

    /**
     * Check whether the result of getSize is an estimate
     * @return true if the size is an estimate
     */
    public boolean isSizeEstimate() {
        return webTable.isSizeEstimate();
    }

    /**
     * Get the exact number of resultElementRows of this table
     * @return the number of resultElementRows
     */
    public int getExactSize() {
        return webTable.getExactSize();
    }
    
    /**
     * Set the rows fields to be a List of Lists of values from ResultElement objects from
     * getResultElementRows().
     */
    private void updateRows() {
        rows = new ArrayList<List<Object>>();
        for (int i = getStartRow(); i < getStartRow() + getPageSize(); i++) {
            try {
                List<Object> newRow = (List<Object>) getAllRows().get(i);
                rows.add(newRow);
            } catch (IndexOutOfBoundsException e) {
                // we're probably at the end of the results object, so stop looping
                break;
            }
        }
    }

    /**
     * Update the internal row list
     * @throws PageOutOfRangeException if update is unable to get the page we want
     */
    private void updateResultElementRows() throws PageOutOfRangeException {
        List<List<ResultElement>> newRows = new ArrayList<List<ResultElement>>();
        for (int i = getStartRow(); i < getStartRow() + getPageSize(); i++) {
            try {
                List<ResultElement> resultsRow = getAllRows().getResultElements(i);
                newRows.add(resultsRow);
            } catch (IndexOutOfBoundsException e) {
                // we're probably at the end of the results object, so stop looping
                break;
            }
        }
        this.resultElementRows = newRows;
        // clear so that getRows() recreates it
        this.rows = null;
    }

    /**
     * Return the maximum retrievable index for this PagedTable.  This will only ever return less
     * than getExactSize() if the underlying data source has a restriction on the maximum index
     * that can be retrieved.
     * @return the maximum retrieved index
     */
    public int getMaxRetrievableIndex() {
        return webTable.getMaxRetrievableIndex();
    }
    
    /**
     * Return the class from the data model for the data displayed in indexed column.
     * This may be the parent class of a field e.g. if column displays A.field where
     * field is a String and A is a class in the model this method will return A.
     * @param index of column to find type for
     * @return the class or parent class for the indexed column
     */
    public Class getTypeForColumn(int index) {
        return webTable.getColumns().get(index).getType();
    }
    
    /**
     * Set the column names
     * @param columnNames a list of Strings
     */
    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    /**
     * @return the webTable
     */
    public WebTable getWebTable() {
        return webTable;
    }
}
