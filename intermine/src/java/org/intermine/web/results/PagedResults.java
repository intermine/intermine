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
import java.util.List;
import java.util.LinkedList;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.query.QueryHelper;

/**
 * A pageable and configurable table created from the Results object.
 *
 * @author Andrew Varley
 * @author Kim Rutherford
 */
public class PagedResults implements PagedTable
{
    private Results results;

    private List columns = new LinkedList();
    private int start = 0;
    private int pageSize = 10;

    /**
     * Create a new PagedResults object from the given Results object.
     *
     * @param results the Results object
     */
    public PagedResults(Results results) {
        this(results, QueryHelper.getColumnAliases(results.getQuery()));
    }

    /**
     * Create a new PagedResults object from the given Results object.
     *
     * @param results the Results object
     * @param columnNames the headings for the Results columns
     */
    public PagedResults(Results results, List columnNames) {
        this.results = results;

        for (int i = 0; i < columnNames.size(); i++) {
            Column column = new Column();
            column.setName((String) columnNames.get(i));
            column.setIndex(i);
            column.setVisible(true);
            columns.add(column);
        }
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

        for (int i = 0; i < columns.size(); i++) {
            Column thisColumn = (Column) columns.get(i);
            if (thisColumn.isVisible())  {
                count++;
            }
        }

        return count;
    }

    /**
     * Return the width (number of columns) of the table.  Used by the JSP because
     * getColumns().size() isn't possible in JSTL.
     * @return the table width
     */
    public int getTableWidth() {
        return getColumns().size();
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
     * Get the underlying results object
     *
     * @return the underlying results object
     */
    public Results getResults() {
        return results;
    }

    /**
     * Get the approximate size of the underlying Results object
     *
     * @return the approximate size of the underlying Results object
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     */
    public int getEstimatedSize() throws ObjectStoreException {
        // Force the underlying results to check that the end is not
        // within this page, or at the end of this page. If it is, the
        // results object will now know the exact size.
        try {
            results.range(start, start + pageSize);
        } catch (IndexOutOfBoundsException e) {
        }

        return results.getInfo().getRows();
    }

    /**
     * Gets whether or not the size is an estimate
     *
     * @return true if size is an estimate
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     */
    public boolean isSizeEstimate() throws ObjectStoreException {
        return !(results.getInfo().getStatus() == ResultsInfo.SIZE);
    }

    /**
     * Get the exact size of the underlying object.
     *
     * @return the exact size of the underlying Results object
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     */
    public int getExactSize() throws ObjectStoreException {
        return getResults().size();
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
        int size = getEstimatedSize();
        if (isSizeEstimate()) {
            // If we were on the end, size would not be an estimate
            return true;
        }
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
        return getResults();
    }
}
