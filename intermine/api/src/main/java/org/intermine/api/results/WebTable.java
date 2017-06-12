package org.intermine.api.results;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Map;

import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.results.flatouterjoins.MultiRow;
import org.intermine.api.results.flatouterjoins.MultiRowValue;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathQuery;

/**
 * A List that can understand ResultElement objects.
 * @author Kim Rutherford
 */
public interface WebTable extends List<MultiRow<ResultsRow<MultiRowValue<ResultElement>>>>
{
    /**
     * Returns true if the results are known to be contained in a single batch. If true, then it
     * would not be sensible to perform precomputing.
     *
     * @return a boolean
     */
    boolean isSingleBatch();

    /**
     * Return a List containing a ResultElement object for each element given in the given row.
     * @param index the row of the results to fetch
     * @return the results row
     */
    MultiRow<ResultsRow<MultiRowValue<ResultElement>>> getResultElements(int index);

    /**
     * Returns the Column objects for this table.
     * @return the columns
     */
    List<Column> getColumns();

    /**
     * Check whether the result of getEstimatedSize() is an estimate
     * @return true if the size is an estimate
     */
    boolean isSizeEstimate();

    /**
     * Get the estimated number of rows of this table
     * @return the number of rows
     */
    int getEstimatedSize();

    /**
     * Return the maximum retrievable index for this PagedTable.  This will only ever return less
     * than getExactSize() if the underlying data source has a restriction on the maximum index
     * that can be retrieved.
     * @return the maximum retrieved index
     */
    int getMaxRetrievableIndex();

    /**
     * Returns the pathToBagQueryResult Map.
     *
     * @return a Map
     */
    Map<String, BagQueryResult> getPathToBagQueryResult();

    /**
     * Get the PathQuery associated with this WebTable
     * @return the PathQuery
     */
    PathQuery getPathQuery();

    /**
     * Gets the underlying results object info
     * @return the ResultsInfo
     * @throws ObjectStoreException exception
     */
    ResultsInfo getInfo() throws ObjectStoreException;

    /**
     * @return The paths for the displayed columns.
     */
    List<Path> getColumnsPath();

    /**
     * Adds columns that should be displayed to the table.
     * @param columnPaths columns correspond to paths and columns for these paths should be added
     */
    void addColumns(List<Path> columnPaths);

}
