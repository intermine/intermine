package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.intermine.web.logic.Constants;

/**
 * A pageable and configurable table of data.
 *
 * @author Andrew Varley
 * @author Kim Rutherford
 */
public class PagedTable
{
    private WebTable webTable;
    private List<String> columnNames = null;
    private List<List<ResultElement>> resultElementRows = null;
    private int startRow = 0;

    private int pageSize = Constants.DEFAULT_TABLE_SIZE;
    private List<Column> columns;
    private String tableid;

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
     * Construct a PagedTable with a list of column names
     * @param webTable the WebTable that this PagedTable will display
     * @param pageSize the number of records to show on each page.  Default value is 10.
     */
    public PagedTable(WebTable webTable, int pageSize) {
        super();
        this.webTable = webTable;
        this.pageSize = pageSize;
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
            Iterator<Column> iter = getColumns().iterator();
            while (iter.hasNext()) {
                String columnName = iter.next().getName();
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
     */
    public void setPageSize(int pageSize) {
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
     */
    public void setPageAndPageSize(int page, int size) {
        this.pageSize = size;
        this.startRow = size * page;
        updateResultElementRows();
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
        updateResultElementRows();
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
        updateResultElementRows();
    }

    /**
     * Check if we are on the last page
     * @return true if we are on the last page
     */
    public boolean isLastPage() {
        return (!isSizeEstimate() && getEndRow() == getEstimatedSize() - 1);
    }

    /**
     * Go to the previous page
     */
    public void previousPage() {
        if (startRow >= pageSize) {
            startRow -= pageSize;
        }
        updateResultElementRows();
    }

    /**
     * Go to the next page
     */
    public void nextPage() {
        startRow += pageSize;
        updateResultElementRows();
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
            updateResultElementRows();
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
    public int getEstimatedSize() {
        return webTable.getEstimatedSize();
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
        return webTable.size();
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
     */
    private void updateResultElementRows() {
        List<List<ResultElement>> newRows = new ArrayList<List<ResultElement>>();
        if (getStartRow() < 0 || getStartRow() > getExactSize()) {
            throw new PageOutOfRangeException("Invalid start row of table.");
        }
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

    /**
     * @return the tableid
     */
    public String getTableid() {
        return tableid;
    }

    /**
     * @param tableid the tableid to set
     */
    public void setTableid(String tableid) {
        this.tableid = tableid;
    }

    private List rearrangedResults = null;

    /**
     * Returns indexes of columns, that should be displayed. 
     * @return indexes
     */
    public List<Integer> getDisplayedIndexes() {
        List<Integer> ret = new ArrayList<Integer>();
        List<Column> columns = getColumns();

        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i) != null && columns.get(i).isVisible()) {
                ret.add(columns.get(i).getIndex());
            }
        }
        
        return ret;
    }
    
    /**
     * Returns a List containing the results, with the columns rearranged.
     *
     * @return a List of rows, each of which is a List
     */
    public List<List<ResultElement>> getRearrangedResults() {
        return new RearrangedList();
    }

    private class RearrangedList extends AbstractList
    {
        private List<Integer> visibleIndexes;

        public RearrangedList() {
            visibleIndexes = getDisplayedIndexes();
        }

        public List<ResultElement> get(int index) {
            return translateRow(webTable.getResultElements(index));
        }

        private List<ResultElement> translateRow(List<ResultElement> row) {
            List<ResultElement> ret = new ArrayList<ResultElement>();
            for (Integer index : visibleIndexes) {
                ret.add(row.get(visibleIndexes.get(index)));
            }
            return ret;
        }

        public int size() {
            return webTable.size();
        }

        public Iterator iterator() {
            return new Iter();
        }

        private class Iter implements Iterator
        {
            private Iterator subIter = webTable.iterator();

            public boolean hasNext() {
                return subIter.hasNext();
            }

            public Object next() {
                List originalRow = (List) subIter.next();
                return translateRow(originalRow);
            }

            public void remove() {
                throw (new UnsupportedOperationException());
            }
        }
        
        private List<Object> reorderRow(int[] columnOrder, boolean[] columnVisible,
                List<Object> row) {
            List<Object> realRow = new ArrayList<Object>();

            for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
                int realColumnIndex;

                if (columnOrder == null) {
                    realColumnIndex = columnIndex;
                } else {
                    realColumnIndex = columnOrder[columnIndex];
                }

                Object o = row.get(realColumnIndex);

                if (columnVisible != null && !columnVisible[columnIndex]) {
                    continue;
                }

                realRow.add(o);
            }
            return realRow;
        }
        
        /**
         * Return an int array containing the real column indexes to use while writing the given
         * PagedTable.
         * @param pt the PagedTable to export
         */
        private int [] getOrder() {
            List columns = getColumns();

            int [] returnValue = new int [columns.size()];

            for (int i = 0; i < columns.size(); i++) {
                returnValue[i] = ((Column) columns.get(i)).getIndex();
            }

            return returnValue;
        }

        /**
         * Return an array containing the visibility of each column in the output
         * @param pt the PagedTable to export
         */
        private boolean [] getVisible() {
            List columns = getColumns();

            boolean [] returnValue = new boolean [columns.size()];

            for (int i = 0; i < columns.size(); i++) {
                returnValue[i] = ((Column) columns.get(i)).isVisible();
            }

            return returnValue;
        }


    }
}
