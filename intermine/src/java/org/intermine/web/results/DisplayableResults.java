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
 * Displayable section of a Results object, containing various
 * bits of configuration information
 *
 * @author Andrew Varley
 */
public class DisplayableResults
{
    // Map to quickly look up columns from aliases
    protected Map aliasToColumn = new HashMap();

    // The columns and their order
    protected List columns = new LinkedList();
    protected int start = 0;
    protected int pageSize = 10;

    protected Results results;

    /**
     * Constructor
     *
     * @param results the results object to configure
     */
    public DisplayableResults(Results results) {
        this.results = results;

        // Add some blank column configurations
        Iterator columnIter = QueryHelper.getColumnAliases(results.getQuery()).iterator();
        int i = 0;
        while (columnIter.hasNext()) {
            String alias = (String) columnIter.next();
            Column column = new Column();
            column.setAlias(alias);
            column.setIndex(i++);
            aliasToColumn.put(alias, column);
            columns.add(column);
        }
    }

    /**
     * Update from another DisplayableResults
     *
     * @param other the DisplayableResults to update from
     */
    public void update(DisplayableResults other) {
        setStart(other.getStart());
        setPageSize(other.getPageSize());

        // For now we are not dealing with adding more columns. If
        // more have been added, then just return, ie. only update
        // start and pageSize
        if (!(getColumns().containsAll(other.getColumns())
              && other.getColumns().containsAll(getColumns()))) {
            return;
        }

        Iterator columnIter = other.getColumns().iterator();
        List newColumns = new LinkedList();
        while (columnIter.hasNext()) {
            Column otherCol = (Column) columnIter.next();
            Column thisCol = (Column) aliasToColumn.get(otherCol.getAlias());
            thisCol.update(otherCol);
            newColumns.add(thisCol);
        }
        // Set the ordering
        columns = newColumns;
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
    public Column getColumn(String alias) {
        return (Column) aliasToColumn.get(alias);
    }

    /**
     * Move a column up in the display order
     *
     * @param alias the alias of the column to move
     */
    public void moveColumnUp(String alias) {
        Column column = getColumn(alias);
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
        Column column = getColumn(alias);
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
        int size = getSize();
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
     * Get the size of the underlying results object
     * NOTE: this may be approximate
     *
     * @return the size of the underlying results object
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     */
    public int getSize() throws ObjectStoreException {
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
     * Gets whether or not there could be any previous rows
     *
     * @return true if the "previous" button should be shown
     */
    public boolean isPreviousButton() {
        return (start > 0);
    }

    /**
     * Gets whether or not there could be more rows
     *
     * @return true if the "next" button should be shown
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     */
    public boolean isNextButton() throws ObjectStoreException {
        int size = getSize();
        if (isSizeEstimate()) {
            // If we were on the end, size would not be an estimate
            return true;
        }
        if (size == (getEnd() + 1)) {
            return false;
        }
        return true;
    }

}
