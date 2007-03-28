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
import java.util.List;

import org.intermine.objectstore.query.ResultsInfo;

import org.intermine.objectstore.ObjectStoreException;

import java.io.Serializable;

/**
 * A pageable and configurable table created from the Results object.
 *
 * @author Andrew Varley
 * @author Kim Rutherford
 */
public class PagedResults extends PagedTable implements Serializable
{
    protected transient WebResults results;

    /**
     * Create a new PagedResults object from the given Results object.
     *
     * @param results the Results object
     */
    public PagedResults(WebResults results) {
        super(results.getColumns());
        this.results = results;

        updateRows();
    }

    /**
     * @see PagedTable#getAllRows()
     */
    public WebColumnTable getAllRows() {
        return results;
    }

    /**
     * @see PagedTable#getSize()
     */
    public int getSize() {
        try {
            return results.getInfo().getRows();
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see PagedTable#isSizeEstimate()
     */
    public boolean isSizeEstimate() {
        try {
            return results.getInfo().getStatus() != ResultsInfo.SIZE;
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see PagedTable#getExactSize()
     */
    public int getExactSize() {
        return results.size();
    }


    /**
     * Return information about the results
     * @return the relevant ResultsInfo
     * @throws ObjectStoreException if an error occurs accessing the underlying ObjectStore
     */
    public ResultsInfo getResultsInfo() throws ObjectStoreException {
        return results.getInfo();
    }

    /**
     * @see PagedTable#getMaxRetrievableIndex()
     */
    public int getMaxRetrievableIndex() {
        return results.getMaxRetrievableIndex();
    }

    /**
     * @see PagedTable#updateRows()
     */
    public void updateRows() {
        List newRows = new ArrayList();
        for (int i = getStartRow(); i < getStartRow() + getPageSize(); i++) {

            try {
                List resultsRow = results.getResultElements(i);
                newRows.add(resultsRow);
            } catch (IndexOutOfBoundsException e) {
                // we're probably at the end of the results object, so stop looping
                break;
            }
        }
        setRows(newRows);
    }
}
