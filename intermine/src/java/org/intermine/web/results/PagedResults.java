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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;

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
    // Map to quickly look up columns from aliases
    private Map aliasToColumn = new HashMap();

    // The columns and their order
    private List columns = new LinkedList();
    private int start = 0;
    private int pageSize = 10;

    private Results results;

    /**
     * Create a new PagedResults object from the given Results object.
     *
     * @param results the Results object
     */
    public PagedResults(Results results) {
        this.results = results;

        // Add some blank column configurations
        Iterator columnIter = QueryHelper.getColumnAliases(results.getQuery()).iterator();
        int i = 0;
        while (columnIter.hasNext()) {
            String alias = (String) columnIter.next();
            Column column = new Column();
            column.setName(alias);
            column.setIndex(i++);
            aliasToColumn.put(alias, column);
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
     * Get the list of column configurations
     *
     * @param alias the alias of the column to get
     * @return the column for the given alias
     */
    public Column getColumnByName(String alias) {
        return (Column) aliasToColumn.get(alias);
    }

    /**
     * Move a column up in the display order
     *
     * @param alias the alias of the column to move
     */
    public void moveColumnUp(String alias) {
        Column column = getColumnByName(alias);
        int index = columns.indexOf(column);
        if (index > 0) {
            columns.remove(index);
            columns.add(index - 1, column);
        }
    }

    /**
     * Move a column down in the display order
     *
     * @param alias the alias of the column to move
     */
    public void moveColumnDown(String alias) {
        Column column = getColumnByName(alias);
        int index = columns.indexOf(column);
        if ((index != -1) && (index < (columns.size() - 1))) {
            columns.remove(index);
            columns.add(index + 1, column);
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
