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
import java.util.Iterator;

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
    private int startIndex = 0;
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
     * @see PagedTable#getColumns
     */
    public List getColumns() {
        return Collections.unmodifiableList(columns);
    }

    /**
     * @see PagedTable#getVisibleColumnCount
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
     * @see PagedTable#getColumnCount
     */
    public int getColumnCount() {
        return columns.size();
    }

    /**
     * @see PagedTable#moveColumnLeft
     */
    public void moveColumnLeft(int index) {
        if (index > 0 && index <= columns.size() - 1) {
            columns.add(index - 1, columns.remove(index));
        }
    }

    /**
     * @see PagedTable#moveColumnRight
     */
    public void moveColumnRight(int index) {
        if (index >= 0 && index < columns.size() - 1) {
            columns.add(index + 1, columns.remove(index));
        }
    }

    /**
     * @see PagedTable#getStartIndex
     */
    public int getStartIndex() {
        return this.startIndex;
    }

    /**
     * @see PagedTable#setStartIndex
     */
    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    /**
     * @see PagedTable#getPageSize
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * @see PagedTable#setPageSize
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * @see PagedTable#getEnd
     */
    public int getEndIndex() {
        int end = startIndex + pageSize - 1;
        if (!isSizeEstimate() && (end + 1 > getSize())) {
            return getSize() - 1;
        } else {
            return end;
        }
    }

    /**
     * @see PagedTable#isPreviousRows
     */
    public boolean isFirstPage() {
        return (startIndex == 0);
    }

    /**
     * @see PagedTable#isMoreRows
     */
    public boolean isLastPage() {
        return (!isSizeEstimate() && getEndIndex() == getSize() - 1);
    }

    /**
     * @see PagedTable#getList
     */
    public List getList() {
        return results;
    }

    /**
     * @see PagedTable#getSize
     */
    public int getSize() {
        //this ensures that if we're on the last page then we get an exact count
        try {
            results.range(startIndex, startIndex + pageSize);
        } catch (IndexOutOfBoundsException e) {
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }

        try {
            return results.getInfo().getRows();
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see PagedTable#isSizeEstimate
     */
    public boolean isSizeEstimate() {
        try {
            return results.getInfo().getStatus() != ResultsInfo.SIZE;
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return information about the results
     * @return the relevant ResultsInfo
     * @throws ObjectStoreException if an error occurs accessing the underlying ObjectStore
     */
    public ResultsInfo getResultsInfo() throws ObjectStoreException {
        return results.getInfo();
    }
}
