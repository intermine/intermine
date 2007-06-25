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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.web.logic.bag.BagQueryResult;
import org.intermine.web.logic.query.PathQuery;

/**
 * A simple WebTable that wraps an arbitrary Results object.
 * @author Xavier Watkins
 */
public class WebResultsSimple extends AbstractList implements WebTable
{
    private Results results;
    private List<String> columnNames;
    private List<Column> columns;
    
    /**
     * Make a new WebResultsSimple object - a simple wrapper around an arbitrary Results object
     * @param results the Results
     * @param columnNames the columns names
     * 
     */
    public WebResultsSimple(Results results, List<String> columnNames) {
        this.results = results;
        this.columnNames = columnNames;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, BagQueryResult> getPathToBagQueryResult() {
        throw new UnsupportedOperationException();
    }

    /** 
     * {@inheritDoc} 
     */
    @Override
    public Object get(int index) {
        return results.get(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        try {
            return results.getInfo().getRows();
        } catch (ObjectStoreException e) {
            throw new RuntimeException("failed to get a ResultsInfo object", e);
        }
    }

    private List<Column> getColumnsInternal() {
        if (columns == null) {
            columns = new ArrayList<Column>();
            for (int i = 0; i < columnNames.size(); i++) {
                String columnName = columnNames.get(i);
                columns.add(new Column(columnName, i, Object.class));
            }
        }
        return columns;
    }

    /**
     * {@inheritDoc}
     */
    public List<Column> getColumns() {
        return getColumnsInternal();
    }

    /**
     * Return a List of ResultElement objects for the row given by index.
     * @param index the row index
     * @return the List
     */
    public List<ResultElement> getResultElements(int index) {
        ResultsRow resultsRow = (ResultsRow) results.get(index);
        List<ResultElement> rowCells = new ArrayList<ResultElement>();
        for (Iterator iter = resultsRow.iterator(); iter.hasNext();) {
            ResultElement resultElement = new ResultElement(iter.next());
            rowCells.add(resultElement);
        }
        return rowCells;
    }

    /**
     * Returns the ObjectStore's maximum allowable offset.
     *
     * @return an int
     */
    public int getMaxRetrievableIndex() {
        return results.getObjectStore().getMaxOffset();
    }

    /**
    /**
     * {@inheritDoc}
     */
    public boolean isSizeEstimate() {
        try {
            return results.getInfo().getStatus() != ResultsInfo.SIZE;
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getExactSize() {
        return results.size();
    }

    public PathQuery getPathQuery() {
        // TODO Auto-generated method stub
        return null;
    }

}
